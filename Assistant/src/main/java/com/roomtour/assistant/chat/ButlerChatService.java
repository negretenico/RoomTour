package com.roomtour.assistant.chat;

import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.lifelog.LifelogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ButlerChatService implements ChatService<ButlerResponse, ButlerRequest> {

    public static final int MAX_TURNS = 5;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final LifelogService lifelogService;
    private final ClaudeClient claudeClient;
    private final ButlerProperties props;

    private final Map<String, Deque<Message>> sessions     = new ConcurrentHashMap<>();
    private final Map<String, String>          sessionRooms = new ConcurrentHashMap<>();

    public ButlerChatService(LifelogService lifelogService, ClaudeClient claudeClient, ButlerProperties props) {
        this.lifelogService = lifelogService;
        this.claudeClient   = claudeClient;
        this.props          = props;
    }

    @Override
    public ButlerResponse chat(ButlerRequest request) {
        String sessionId = Optional.ofNullable(request.sessionId())
            .filter(s -> !s.isBlank())
            .orElse(UUID.randomUUID().toString());

        Deque<Message> history = sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>());

        String lastRoom = sessionRooms.get(sessionId);
        if (lastRoom != null && !lastRoom.equals(request.room())) {
            history.addLast(new Message("assistant", "[I've moved to the " + request.room() + "]"));
        }
        sessionRooms.put(sessionId, request.room());

        history.addLast(new Message("user", request.message()));

        while (history.size() > MAX_TURNS * 2) {
            history.removeFirst();
        }

        log.debug("Routing chat: sessionId={}, room={}", sessionId, request.room());
        return claudeClient.send(buildSystemPrompt(request.room()), List.copyOf(history))
            .map(text -> {
                history.addLast(new Message("assistant", text));
                return new ButlerResponse(text, sessionId);
            })
            .onFailure(e -> log.error("Chat failed for sessionId={}: {}", sessionId, e.getMessage(), e))
            .getOrElse(() -> new ButlerResponse("I'm sorry, I couldn't process that right now.", sessionId));
    }

    private String buildSystemPrompt(String room) {
        String time = LocalTime.now().format(TIME_FMT);
        return String.format(
            "You are %s, %s's home drone butler. You are %s.%n" +
            "You are currently in the %s. The time is %s.%n%n" +
            "Here is %s's recent context:%n%s%n%n" +
            "Answer naturally, as if you've been with them all day. " +
            "Keep responses concise and conversational — you will be speaking aloud.",
            props.getName(), props.getUserName(), props.getPersonality(),
            room, time,
            props.getUserName(), lifelogService.formatForPrompt()
        );
    }
}

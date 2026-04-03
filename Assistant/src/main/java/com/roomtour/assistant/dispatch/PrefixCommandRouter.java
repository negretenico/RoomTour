package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Routes requests by prefix: messages starting with '/' are dispatched as commands
 * immediately (single-shot, no history). All other messages delegate to the
 * conversation service.
 */
public class PrefixCommandRouter implements CommandRouter {

    private final ChatService<ButlerResponse, ButlerRequest> chatService;
    private final LifelogService lifelogService;
    private final ClaudeClient claudeClient;
    private final ButlerProperties props;

    public PrefixCommandRouter(ChatService<ButlerResponse, ButlerRequest> chatService,
                                LifelogService lifelogService,
                                ClaudeClient claudeClient,
                                ButlerProperties props) {
        this.chatService    = chatService;
        this.lifelogService = lifelogService;
        this.claudeClient   = claudeClient;
        this.props          = props;
    }

    @Override
    public ButlerResponse route(ButlerRequest request) {
        String message = request.message().strip();
        if (!message.startsWith("/")) {
            return chatService.chat(request);
        }

        String sessionId = Optional.ofNullable(request.sessionId())
            .filter(s -> !s.isBlank())
            .orElse(UUID.randomUUID().toString());

        return switch (commandToken(message)) {
            case "/whats-new"   -> whatsNew(sessionId);
            case "/brief"       -> brief(sessionId);
            case "/add-note"    -> addNote(message, sessionId);
            case "/where-am-i"  -> whereAmI(request.room(), sessionId);
            case "/commands"    -> commands(sessionId);
            default             -> unknown(commandToken(message), sessionId);
        };
    }

    private String commandToken(String message) {
        int space = message.indexOf(' ');
        return space == -1 ? message : message.substring(0, space);
    }

    private ButlerResponse whatsNew(String sessionId) {
        String context = lifelogService.formatForPrompt();
        String response = context.isBlank()
            ? "Nothing new in your lifelog."
            : "Here's what's going on:\n" + context;
        return new ButlerResponse(response, sessionId);
    }

    private ButlerResponse brief(String sessionId) {
        String prompt = String.format(
            "You are %s, %s's home drone butler. Give a concise daily brief based on this context:%n%s",
            props.getName(), props.getUserName(), lifelogService.formatForPrompt()
        );
        return claudeClient.send(prompt, List.of())
            .map(text -> new ButlerResponse(text, sessionId))
            .getOrElse(() -> new ButlerResponse("Unable to generate brief right now.", sessionId));
    }

    private ButlerResponse addNote(String message, String sessionId) {
        String note = message.substring("/add-note".length()).strip();
        if (note.isBlank()) {
            return unknown("/add-note", sessionId);
        }
        lifelogService.addNote(note);
        return new ButlerResponse("Got it, I've noted: \"" + note + "\"", sessionId);
    }

    private ButlerResponse commands(String sessionId) {
        String list = "/commands | /where-am-i | /whats-new | /brief | /add-note <text>";
        return new ButlerResponse(list, sessionId);
    }

    private ButlerResponse whereAmI(String room, String sessionId) {
        String location = (room == null || room.isBlank() || room.equals("unknown"))
            ? "an unrecognised room" : room;
        return new ButlerResponse("You are in the " + location + ".", sessionId);
    }

    private ButlerResponse unknown(String token, String sessionId) {
        return new ButlerResponse("Unknown command: " + token, sessionId);
    }
}

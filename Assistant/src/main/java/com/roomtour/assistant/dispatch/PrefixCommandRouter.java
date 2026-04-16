package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.command.ButlerCommand;
import com.roomtour.assistant.dispatch.command.MapCommand;
import com.common.functionico.value.Maybe;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Routes requests by prefix: messages starting with '/' are dispatched to the matching
 * {@link ButlerCommand}. Free-text delegates to the conversation service — unless the
 * session is in MAP_BUILDING mode, in which case it is forwarded to {@link MapCommand}.
 */
@Slf4j
public class PrefixCommandRouter implements CommandRouter {

    private final ChatService<ButlerResponse, ButlerRequest> chatService;
    private final List<ButlerCommand> commands;
    private final MapCommand mapCommand;

    public PrefixCommandRouter(ChatService<ButlerResponse, ButlerRequest> chatService,
                                List<ButlerCommand> commands,
                                MapCommand mapCommand) {
        this.chatService = chatService;
        this.commands    = commands;
        this.mapCommand  = mapCommand;
    }

    @Override
    public ButlerResponse route(ButlerRequest request) {
        String message   = request.message().strip();
        String sessionId = resolveSessionId(request.sessionId());

        if (mapCommand.isSessionActive(sessionId) && !message.startsWith("/")) {
            return mapCommand.handleFreeText(message, sessionId);
        }

        if (!message.startsWith("/")) {
            return matchIntent(message, sessionId)
                .orElseGet(() -> {
                    log.info("[ROUTER] no intent match for '{}' — delegating to ChatService", message);
                    return chatService.chat(request);
                });
        }

        String token = commandToken(message);

        if ("/commands".equals(token)) {
            String list = commands.stream().map(ButlerCommand::usage).collect(Collectors.joining(" | "));
            return new ButlerResponse(list, sessionId);
        }

        return commands.stream()
            .filter(c -> c.token().equals(token))
            .findFirst()
            .map(c -> dispatchPrefix(c, token, message, sessionId))
            .orElseGet(() -> new ButlerResponse("Unknown command: " + token, sessionId));
    }

    private Optional<ButlerResponse> matchIntent(String message, String sessionId) {
        return commands.stream()
            .filter(c -> matchesIntent(c, message))
            .findFirst()
            .map(c -> dispatchIntent(c, message, sessionId));
    }

    private boolean matchesIntent(ButlerCommand command, String message) {
        return command.intentPattern()
            .map(p -> p.matcher(message).find())
            .orElse(false);
    }

    private ButlerResponse dispatchIntent(ButlerCommand command, String message, String sessionId) {
        log.info("[ROUTER] intent match '{}' → {}", message, command.getClass().getSimpleName());
        return command.intentExecute(message, sessionId);
    }

    private ButlerResponse dispatchPrefix(ButlerCommand command, String token, String message, String sessionId) {
        log.info("[ROUTER] prefix match '{}' → {}", token, command.getClass().getSimpleName());
        return command.execute(message, sessionId);
    }

    private String resolveSessionId(String sessionId) {
        return Maybe.of(sessionId)
            .map(s -> s.isBlank() ? null : s)
            .orElse(UUID.randomUUID().toString());
    }

    private String commandToken(String message) {
        int space = message.indexOf(' ');
        return space == -1 ? message : message.substring(0, space);
    }
}

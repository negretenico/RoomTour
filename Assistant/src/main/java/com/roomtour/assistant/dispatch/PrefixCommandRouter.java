package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.command.ButlerCommand;
import com.roomtour.assistant.dispatch.command.MapCommand;
import com.common.functionico.value.Maybe;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Routes requests by prefix: messages starting with '/' are dispatched to the matching
 * {@link ButlerCommand}. Free-text delegates to the conversation service — unless the
 * session is in MAP_BUILDING mode, in which case it is forwarded to {@link MapCommand}.
 */
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
            return chatService.chat(request);
        }

        String token = commandToken(message);

        if ("/commands".equals(token)) {
            String list = commands.stream().map(ButlerCommand::usage).collect(Collectors.joining(" | "));
            return new ButlerResponse(list, sessionId);
        }

        return commands.stream()
            .filter(c -> c.token().equals(token))
            .findFirst()
            .map(c -> c.execute(message, sessionId))
            .orElseGet(() -> new ButlerResponse("Unknown command: " + token, sessionId));
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

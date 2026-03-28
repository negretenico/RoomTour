package com.roomtour.server.controller;

import com.common.functionico.value.Maybe;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.server.model.chat.ChatRequest;
import com.roomtour.server.model.chat.ChatResponse;
import com.roomtour.server.session.RoomSessionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/chat", produces = APPLICATION_JSON_VALUE)
public class ChatController {

    private final CommandRouter commandRouter;
    private final RoomSessionStore roomSessionStore;

    public ChatController(CommandRouter commandRouter, RoomSessionStore roomSessionStore) {
        this.commandRouter = commandRouter;
        this.roomSessionStore = roomSessionStore;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Maybe.of(request.message())
            .map(m -> m.isBlank() ? null : m)
            .map(message -> {
                String sessionId = resolveSessionId(request.sessionId());
                String room = roomSessionStore.getOrDefault(sessionId, "unknown");
                var butlerResponse = commandRouter.route(new ButlerRequest(message, room, sessionId));
                return ResponseEntity.ok(new ChatResponse(butlerResponse.response(), butlerResponse.sessionId()));
            })
            .orElse(ResponseEntity.badRequest().build());
    }

    private String resolveSessionId(String sessionId) {
        return Maybe.of(sessionId)
            .map(s -> s.isBlank() ? null : s)
            .orElse(UUID.randomUUID().toString());
    }
}

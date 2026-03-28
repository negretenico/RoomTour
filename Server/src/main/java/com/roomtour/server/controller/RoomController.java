package com.roomtour.server.controller;

import com.common.functionico.value.Maybe;
import com.roomtour.server.model.room.RoomRequest;
import com.roomtour.server.model.room.RoomResponse;
import com.roomtour.server.session.RoomSessionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/room", produces = APPLICATION_JSON_VALUE)
public class RoomController {

    private final RoomSessionStore roomSessionStore;

    public RoomController(RoomSessionStore roomSessionStore) {
        this.roomSessionStore = roomSessionStore;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> setRoom(@RequestBody RoomRequest request) {
        return Maybe.of(request.room())
            .map(r -> r.isBlank() ? null : r)
            .map(room -> {
                String sessionId = resolveSessionId(request.sessionId());
                roomSessionStore.put(sessionId, room);
                return ResponseEntity.ok(new RoomResponse(sessionId, room));
            })
            .orElse(ResponseEntity.badRequest().build());
    }

    private String resolveSessionId(String sessionId) {
        return Maybe.of(sessionId)
            .map(s -> s.isBlank() ? null : s)
            .orElse(UUID.randomUUID().toString());
    }
}

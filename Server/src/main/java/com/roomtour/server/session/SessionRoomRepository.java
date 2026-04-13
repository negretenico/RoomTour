package com.roomtour.server.session;

import com.roomtour.assistant.core.model.CurrentRoomRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRoomRepository implements CurrentRoomRepository {

    private final RoomSessionStore store;

    public SessionRoomRepository(RoomSessionStore store) {
        this.store = store;
    }

    @Override
    public String getCurrentRoom(String sessionId) {
        return store.getOrDefault(sessionId, "unknown");
    }
}

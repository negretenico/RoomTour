package com.roomtour.server.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe store mapping session IDs to their current room. */
@Component
public class RoomSessionStore {

    private final Map<String, String> roomBySession = new ConcurrentHashMap<>();

    public void put(String sessionId, String room) {
        roomBySession.put(sessionId, room);
    }

    public String getOrDefault(String sessionId, String defaultRoom) {
        return roomBySession.getOrDefault(sessionId, defaultRoom);
    }
}

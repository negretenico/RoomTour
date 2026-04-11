package com.roomtour.assistant.navigation;

import com.common.functionico.value.Maybe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active map-building sessions per sessionId.
 * Each session holds a GraphBuildingService being built up over multiple turns.
 * Sessions expire after TIMEOUT of inactivity.
 */
@Component
public class MapBuildingSession {

    static final Duration TIMEOUT = Duration.ofMinutes(5);

    private record ActiveSession(GraphBuildingService service, Instant lastActivity) {
        boolean isExpired() {
            return Instant.now().isAfter(lastActivity.plus(TIMEOUT));
        }
        ActiveSession touch() {
            return new ActiveSession(service, Instant.now());
        }
    }

    private final Map<String, ActiveSession> sessions = new ConcurrentHashMap<>();

    public void start(String sessionId, GraphBuildingService service) {
        sessions.put(sessionId, new ActiveSession(service, Instant.now()));
    }

    public boolean isActive(String sessionId) {
        ActiveSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.isExpired()) {
            sessions.remove(sessionId);
            return false;
        }
        return true;
    }

    public Maybe<GraphBuildingService> getService(String sessionId) {
        ActiveSession session = sessions.get(sessionId);
        if (session == null || session.isExpired()) return Maybe.none();
        sessions.put(sessionId, session.touch());
        return Maybe.of(session.service());
    }

    public void end(String sessionId) {
        sessions.remove(sessionId);
    }
}

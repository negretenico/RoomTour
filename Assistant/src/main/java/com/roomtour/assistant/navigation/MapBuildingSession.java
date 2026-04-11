package com.roomtour.assistant.navigation;

import com.common.functionico.value.Maybe;
import com.roomtour.assistant.config.NavigationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active map-building sessions per sessionId.
 * Each session holds a GraphBuildingService being built up over multiple turns.
 * Sessions expire after the configured timeout of inactivity.
 */
@Component
public class MapBuildingSession {

    private final Duration timeout;
    private final Map<String, ActiveSession> sessions = new ConcurrentHashMap<>();

    public MapBuildingSession(NavigationProperties props) {
        this.timeout = Duration.ofMinutes(props.getSessionTimeoutMinutes());
    }

    public void start(String sessionId, GraphBuildingService service) {
        sessions.put(sessionId, new ActiveSession(service, timeout));
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

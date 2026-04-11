package com.roomtour.assistant.navigation;

import java.time.Duration;
import java.time.Instant;

class ActiveSession {

    private final GraphBuildingService service;
    private final Instant              lastActivity;
    private final Duration             timeout;

    ActiveSession(GraphBuildingService service, Duration timeout) {
        this(service, Instant.now(), timeout);
    }

    private ActiveSession(GraphBuildingService service, Instant lastActivity, Duration timeout) {
        this.service      = service;
        this.lastActivity = lastActivity;
        this.timeout      = timeout;
    }

    boolean isExpired() {
        return Instant.now().isAfter(lastActivity.plus(timeout));
    }

    ActiveSession touch() {
        return new ActiveSession(service, Instant.now(), timeout);
    }

    GraphBuildingService service() {
        return service;
    }
}

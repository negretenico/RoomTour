package com.roomtour.assistant.navigation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Single source of truth for the live RoomGraph.
 * Loads the persisted graph on startup; falls back to an empty graph if none exists.
 * All services that need the current graph should inject this rather than
 * calling GraphPersistenceService.load() directly.
 */
@Slf4j
@Component
public class RoomGraphHolder {

    private final GraphPersistenceService persistence;
    private volatile RoomGraph graph;

    public RoomGraphHolder(GraphPersistenceService persistence) {
        this.persistence = persistence;
    }

    @PostConstruct
    void load() {
        graph = persistence.load()
            .onSuccess(g -> log.info("Room graph loaded: {} room(s), {} connection(s)",
                g.getRooms().size(), g.getAdjacency().values().stream().mapToLong(m -> m.size()).sum() / 2))
            .onFailure(e -> log.info("No persisted room graph found — starting empty"))
            .getOrElse(RoomGraph::new);
    }

    public RoomGraph get() {
        return graph;
    }

    public void set(RoomGraph updated) {
        this.graph = updated;
    }
}

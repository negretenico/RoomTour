package com.roomtour.assistant.navigation;

import com.roomtour.assistant.briefing.SpeakRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.drone.ExploreCompletedEvent;
import com.roomtour.drone.Explorer;
import com.roomtour.drone.RoomDetectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the lifecycle of autonomous exploration.
 * Listens for {@link RoomDetectedEvent}s while active and builds a fresh graph,
 * inferring connections as the drone transitions between rooms.
 * Publishes {@link SpeakRequest}s for proactive TTS announcements.
 */
@Slf4j
@Component
public class ExplorationService {

    private final Explorer explorer;
    private final GraphPersistenceService graphPersistence;
    private final RoomGraphHolder graphHolder;
    private final ApplicationEventPublisher events;

    private final AtomicBoolean exploring = new AtomicBoolean(false);
    private volatile ExplorationGraphBuilder builder;
    private volatile String lastRoom;

    public ExplorationService(Explorer explorer,
                              GraphPersistenceService graphPersistence,
                              RoomGraphHolder graphHolder,
                              ApplicationEventPublisher events) {
        this.explorer         = explorer;
        this.graphPersistence = graphPersistence;
        this.graphHolder      = graphHolder;
        this.events           = events;
    }

    public ButlerResponse start(String sessionId) {
        if (exploring.getAndSet(true)) {
            return new ButlerResponse("Already exploring.", sessionId);
        }
        builder  = new ExplorationGraphBuilder();
        lastRoom = null;
        explorer.start();
        log.info("[EXPLORE] Exploration started");
        return new ButlerResponse("Starting exploration. I will announce each room as I discover it.", sessionId);
    }

    public ButlerResponse stop(String sessionId) {
        if (!exploring.getAndSet(false)) {
            return new ButlerResponse("Not currently exploring.", sessionId);
        }
        explorer.stop();
        String summary = finishExploration();
        return new ButlerResponse("Exploration stopped. " + summary, sessionId);
    }

    public boolean isExploring() {
        return exploring.get();
    }

    @EventListener
    public void onRoomDetected(RoomDetectedEvent event) {
        if (!exploring.get()) return;

        String room           = event.room();
        String normalizedRoom = RoomGraph.normalize(room);

        if (normalizedRoom.equals(lastRoom)) return;

        if (lastRoom != null) {
            builder.addConnection(lastRoom, normalizedRoom, 1.0);
        } else {
            builder.addRoom(room);
        }
        lastRoom = normalizedRoom;

        graphHolder.set(builder.getGraph());
        log.info("[EXPLORE] Room discovered: {}", room);
        events.publishEvent(new SpeakRequest(this, "Found the " + room + "."));
    }

    @EventListener
    public void onExploreCompleted(ExploreCompletedEvent event) {
        if (!exploring.getAndSet(false)) return;
        String summary = finishExploration();
        events.publishEvent(new SpeakRequest(this, summary));
    }

    private String finishExploration() {
        RoomGraph graph = builder.getGraph();
        graphPersistence.save(graph)
            .onSuccess(path -> {
                graphHolder.set(graph);
                log.info("[EXPLORE] Graph saved: {}", graph.summary());
            })
            .onFailure(e -> log.error("[EXPLORE] Failed to save exploration graph", e));
        int count = graph.getRooms().size();
        return "Exploration complete. Found " + count + " room" + (count == 1 ? "" : "s") + ".";
    }
}

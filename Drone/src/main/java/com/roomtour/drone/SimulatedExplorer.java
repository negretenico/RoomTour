package com.roomtour.drone;

import com.roomtour.drone.config.ExplorationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulation-mode explorer — fires synthetic {@link RoomDetectedEvent}s on a fixed interval,
 * then publishes {@link ExploreCompletedEvent} when all configured rooms are exhausted.
 * Activated when {@code butler.ros2.enabled=false}.
 */
@Slf4j
public class SimulatedExplorer implements Explorer {

    private final ExplorationProperties props;
    private final ApplicationEventPublisher events;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> task;

    public SimulatedExplorer(ExplorationProperties props, ApplicationEventPublisher events) {
        this.props  = props;
        this.events = events;
    }

    @Override
    public void start() {
        List<String> rooms = props.getSimulatedRooms();
        log.info("[SIM] Starting simulated exploration: {} rooms at {}ms intervals", rooms.size(), props.getSimulatedRoomIntervalMs());
        AtomicInteger index = new AtomicInteger(0);
        long interval = props.getSimulatedRoomIntervalMs();
        task = scheduler.scheduleAtFixedRate(() -> {
            int i = index.getAndIncrement();
            if (i < rooms.size()) {
                String room = rooms.get(i);
                log.info("[SIM] Simulating room detection: {}", room);
                events.publishEvent(new RoomDetectedEvent(this, room));
            } else {
                task.cancel(false);
                log.info("[SIM] Simulated exploration complete");
                events.publishEvent(new ExploreCompletedEvent(this));
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel(false);
            log.info("[SIM] Simulated exploration stopped by user");
        }
    }
}

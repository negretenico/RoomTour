package com.roomtour.server.controller;

import com.roomtour.drone.ExploreCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Webhook called by the ROS2 side when explore_lite exhausts all frontiers.
 * Fires {@link ExploreCompletedEvent} so {@code ExplorationService} can save the graph
 * and announce completion via TTS.
 */
@RestController
@RequestMapping(value = "/api/v1/explore", produces = APPLICATION_JSON_VALUE)
public class ExploreController {

    private final ApplicationEventPublisher events;

    public ExploreController(ApplicationEventPublisher events) {
        this.events = events;
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> onExplorationComplete() {
        events.publishEvent(new ExploreCompletedEvent(this));
        return ResponseEntity.ok().build();
    }
}

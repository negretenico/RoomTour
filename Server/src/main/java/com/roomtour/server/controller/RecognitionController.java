package com.roomtour.server.controller;

import com.roomtour.recognition.classify.RoomClassifier;
import com.roomtour.recognition.core.model.GeometricSignature;
import com.roomtour.recognition.core.model.RoomObservation;
import com.roomtour.recognition.core.model.SidecarPayload;
import com.roomtour.server.session.RoomSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Receives webhook pushes from the YOLO sidecar service.
 * On each push, classifies the room and updates the global session context.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/recognition", produces = APPLICATION_JSON_VALUE)
public class RecognitionController {

    static final String GLOBAL_SESSION = "__global__";

    private final RoomClassifier roomClassifier;
    private final RoomSessionStore roomSessionStore;

    public RecognitionController(RoomClassifier roomClassifier, RoomSessionStore roomSessionStore) {
        this.roomClassifier = roomClassifier;
        this.roomSessionStore = roomSessionStore;
    }

    @PostMapping("/classify")
    public ResponseEntity<Void> onSidecarPush(@RequestBody SidecarPayload payload) {
        log.info("Processing request {}",payload);
        var observation = new RoomObservation(
            new GeometricSignature(0, 0, 0, 0, 0),
            payload.detectedObjects() != null ? payload.detectedObjects() : List.of()
        );
        roomClassifier.classify(observation)
            .ifPresent(room -> {
                log.info("Room classified: {}", room);
                roomSessionStore.put(GLOBAL_SESSION, room);
            });
        return ResponseEntity.ok().build();
    }
}

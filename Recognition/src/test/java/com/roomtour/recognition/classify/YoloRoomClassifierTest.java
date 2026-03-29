package com.roomtour.recognition.classify;

import com.roomtour.recognition.core.model.DetectedObject;
import com.roomtour.recognition.core.model.GeometricSignature;
import com.roomtour.recognition.core.model.RecognitionProperties;
import com.roomtour.recognition.core.model.RoomObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YoloRoomClassifierTest {

    private RecognitionProperties props;
    private YoloRoomClassifier classifier;

    @BeforeEach
    void setUp() {
        props = new RecognitionProperties();
        props.setRooms(Map.of(
            "kitchen", roomConfig(List.of("stove", "sink", "microwave"), 6, 25),
            "living-room", roomConfig(List.of("sofa", "television"), 15, 50),
            "bathroom", roomConfig(List.of("toilet", "bathtub"), 3, 12)
        ));
        classifier = new YoloRoomClassifier(props);
    }

    @Test
    void classifierReturnsEmptyWhenNoObjectsDetected() {
        var result = classifier.classify(observation(List.of(), noGeometry()));
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void classifierReturnsEmptyWhenObjectsDontMatchAnyRoom() {
        var result = classifier.classify(observation(
            List.of(new DetectedObject("bicycle", 0.95)),
            noGeometry()
        ));
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void classifierIdentifiesRoomFromDetectedObjects() {
        var result = classifier.classify(observation(
            List.of(new DetectedObject("stove", 0.92), new DetectedObject("sink", 0.88)),
            noGeometry()
        ));
        assertThat(result.isPresent()).isTrue();
        assertThat(result.orElse(null)).isEqualTo("kitchen");
    }

    @Test
    void classifierPicksRoomWithHighestCumulativeConfidence() {
        var result = classifier.classify(observation(
            List.of(
                new DetectedObject("stove", 0.4),
                new DetectedObject("sofa", 0.9),
                new DetectedObject("television", 0.85)
            ),
            noGeometry()
        ));
        assertThat(result.orElse(null)).isEqualTo("living-room");
    }

    @Test
    void geometryWinsWhenYoloPredictionConflictsWithFloorPlanBounds() {
        // YOLO says kitchen, but 60m² is way outside kitchen's max of 25m²
        // living-room bounds (15–50m²) also don't fit; no geometry match → empty
        var result = classifier.classify(observation(
            List.of(new DetectedObject("stove", 0.91)),
            new GeometricSignature(10.0, 6.0, 2.5, 0.7, 2) // 60m²
        ));
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void geometryFallsBackToMatchingRoomWhenYoloConflicts() {
        // YOLO says kitchen (stove detected), but 30m² fits living-room (15–50m²), not kitchen (6–25m²)
        var result = classifier.classify(observation(
            List.of(new DetectedObject("stove", 0.91)),
            new GeometricSignature(5.0, 6.0, 2.5, 0.7, 2) // 30m²
        ));
        assertThat(result.isPresent()).isTrue();
        assertThat(result.orElse(null)).isEqualTo("living-room");
    }

    @Test
    void classifierSkipsGeometryCheckWhenDepthDataUnavailable() {
        // No geometry available — YOLO result is trusted directly
        var result = classifier.classify(observation(
            List.of(new DetectedObject("stove", 0.92)),
            noGeometry()
        ));
        assertThat(result.orElse(null)).isEqualTo("kitchen");
    }

    // --- helpers ---

    private RoomObservation observation(List<DetectedObject> objects, GeometricSignature geometry) {
        return new RoomObservation(geometry, objects);
    }

    private GeometricSignature noGeometry() {
        return new GeometricSignature(0, 0, 0, 0, 0);
    }

    private RecognitionProperties.RoomConfig roomConfig(List<String> objects, double min, double max) {
        var config = new RecognitionProperties.RoomConfig();
        config.setObjects(objects);
        config.setMinAreaM2(min);
        config.setMaxAreaM2(max);
        return config;
    }
}

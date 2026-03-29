package com.roomtour.recognition.classify;

import com.roomtour.recognition.core.model.DetectedObject;
import com.roomtour.recognition.core.model.GeometricSignature;
import com.roomtour.recognition.core.model.RecognitionProperties;
import com.roomtour.recognition.core.model.RoomObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedRoomClassifierTest {

    private RecognitionProperties props;
    private SimulatedRoomClassifier classifier;

    @BeforeEach
    void setUp() {
        props = new RecognitionProperties();
        classifier = new SimulatedRoomClassifier(props);
    }

    @Test
    void classifierReturnsConfiguredRoom() {
        props.setRoom("kitchen");
        var result = classifier.classify(anyObservation());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.orElse(null)).isEqualTo("kitchen");
    }

    @Test
    void classifierReturnsEmptyWhenNoRoomConfigured() {
        props.setRoom(null);
        assertThat(classifier.classify(anyObservation()).isPresent()).isFalse();
    }

    @Test
    void classifierIgnoresSensorData() {
        props.setRoom("living room");
        var withObjects = new RoomObservation(
            new GeometricSignature(10.0, 15.0, 3.5, 0.8, 3),
            List.of(new DetectedObject("sofa", 0.92), new DetectedObject("tv", 0.87))
        );
        var empty = new RoomObservation(
            new GeometricSignature(2.0, 2.0, 2.1, 0.3, 1),
            List.of()
        );
        assertThat(classifier.classify(withObjects).orElse(null))
            .isEqualTo(classifier.classify(empty).orElse(null));
    }

    private RoomObservation anyObservation() {
        return new RoomObservation(
            new GeometricSignature(4.0, 5.0, 2.5, 0.6, 2),
            List.of()
        );
    }
}

package com.roomtour.recognition.classify;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.core.model.RecognitionProperties;
import com.roomtour.recognition.core.model.RoomObservation;

/**
 * Simulation-mode classifier — returns the room configured via
 * {@code butler.recognition.room}, ignoring the observation entirely.
 * Returns {@link Maybe#none()} when no room is configured.
 * Swapped for a YOLO + geometry-validated implementation in Phase 2.
 */
public class SimulatedRoomClassifier implements RoomClassifier {

    private final RecognitionProperties props;

    public SimulatedRoomClassifier(RecognitionProperties props) {
        this.props = props;
    }

    @Override
    public Maybe<String> classify(RoomObservation observation) {
        return Maybe.of(props.getRoom())
            .map(r -> r.isBlank() ? null : r);
    }
}

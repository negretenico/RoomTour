package com.roomtour.recognition.classify;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.core.model.RoomObservation;

/**
 * Classifies a room from a combined sensor observation (YOLO detections + depth geometry).
 * Returns {@link Maybe#none()} when the classifier cannot confidently identify the room —
 * callers must handle the unrecognised case.
 */
public interface RoomClassifier {
    Maybe<String> classify(RoomObservation observation);
}

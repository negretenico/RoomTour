package com.roomtour.recognition.classify;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.core.model.DetectedObject;
import com.roomtour.recognition.core.model.GeometricSignature;
import com.roomtour.recognition.core.model.RecognitionProperties;
import com.roomtour.recognition.core.model.RecognitionProperties.RoomConfig;
import com.roomtour.recognition.core.model.RoomObservation;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * YOLO-backed room classifier.
 *
 * <p>Primary signal: sums YOLO detection confidence per configured room type and picks the highest.
 * Sanity check: if the predicted room's floor-plan bounds don't fit the observed geometry,
 * the YAML floor plan is the authority — falls back to whichever room's bounds do fit.
 * Returns {@link Maybe#none()} when neither signal can identify the room confidently.
 */
@Slf4j
public class YoloRoomClassifier implements RoomClassifier {

    private final RecognitionProperties props;

    public YoloRoomClassifier(RecognitionProperties props) {
        this.props = props;
    }

    @Override
    public Maybe<String> classify(RoomObservation observation) {
        return scoreRooms(observation)
            .map(predicted -> validateGeometry(predicted, observation.geometry()))
            .orElse(Maybe.none());
    }

    /** Returns the highest-scoring room label, or empty if no configured objects were detected. */
    private Optional<String> scoreRooms(RoomObservation observation) {
        return props.getRooms().entrySet().stream()
            .map(entry -> Map.entry(entry.getKey(), score(observation, entry.getValue())))
            .filter(entry -> entry.getValue() > 0)
            .max(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey);
    }

    /** Sums the confidence of all detected objects that belong to this room type. */
    private double score(RoomObservation observation, RoomConfig config) {
        return observation.detectedObjects().stream()
            .filter(obj -> config.getObjects().contains(obj.label()))
            .mapToDouble(DetectedObject::confidence)
            .sum();
    }

    /**
     * Validates the YOLO-predicted room against depth-camera geometry.
     * If geometry is unavailable, trusts YOLO directly.
     * If geometry conflicts with the predicted room's bounds, falls back to whichever
     * configured room fits the geometry — or {@link Maybe#none()} if none match.
     */
    private Maybe<String> validateGeometry(String predicted, GeometricSignature geometry) {
        if (!geometry.isAvailable()) {
            log.debug("No geometry available — trusting YOLO: room={}", predicted);
            return Maybe.of(predicted);
        }

        double area = geometry.widthMeters() * geometry.lengthMeters();
        RoomConfig predictedConfig = props.getRooms().get(predicted);

        if (fitsConfig(area, predictedConfig)) {
            log.debug("YOLO and geometry agree: room={}, area={}m²", predicted, area);
            return Maybe.of(predicted);
        }

        log.debug("YOLO ({}) conflicts with geometry ({}m²) — falling back to floor plan", predicted, area);
        return geometryFallback(area);
    }

    /** Finds the first configured room whose area bounds contain the observed floor area. */
    private Maybe<String> geometryFallback(double area) {
        return props.getRooms().entrySet().stream()
            .filter(entry -> fitsConfig(area, entry.getValue()))
            .map(Map.Entry::getKey)
            .findFirst()
            .map(Maybe::of)
            .orElse(Maybe.none());
    }

    private boolean fitsConfig(double area, RoomConfig config) {
        return config != null && area >= config.getMinAreaM2() && area <= config.getMaxAreaM2();
    }
}

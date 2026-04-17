package com.roomtour.recognition.core.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "butler.recognition")
public class RecognitionProperties {

    /** When true, uses {@code SimulatedRoomClassifier} (returns {@code room}). Default. */
    private boolean simulation = true;

    /** Room label returned by the simulated classifier. */
    private String room = "unknown";

    /** Number of frames in the sliding confidence window. */
    private int windowSize = 20;

    /** Fraction of window frames a single label must hold to confirm a room update (0.0–1.0). */
    private double thresholdPercent = 0.80;

    /**
     * Per-room configuration used by the YOLO classifier.
     * Key is the room label (e.g. {@code kitchen}, {@code living-room}).
     */
    private Map<String, RoomConfig> rooms = new HashMap<>();

    @Data
    public static class RoomConfig {
        /** YOLO object labels that indicate this room type (e.g. stove, sink). */
        private List<String> objects = new ArrayList<>();
        /** Minimum floor area in square metres for this room type. */
        private double minAreaM2 = 0;
        /** Maximum floor area in square metres for this room type. */
        private double maxAreaM2 = Double.MAX_VALUE;
    }
}

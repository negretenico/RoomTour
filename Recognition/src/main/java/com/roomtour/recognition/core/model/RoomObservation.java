package com.roomtour.recognition.core.model;

import java.util.List;

/**
 * Everything the classifier knows about the current space:
 * depth-camera geometry (sanity check) and YOLO-detected objects (primary signal).
 *
 * <p>In simulation mode both fields may be empty/dummy — the simulated classifier ignores them.
 * In Phase 2 the real classifier uses {@code detectedObjects} to propose a room label and
 * validates it against {@code geometry} and the YAML floor plan bounds.
 */
public record RoomObservation(
    GeometricSignature geometry,
    List<DetectedObject> detectedObjects
) {}

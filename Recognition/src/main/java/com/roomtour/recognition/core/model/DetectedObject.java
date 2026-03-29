package com.roomtour.recognition.core.model;

/**
 * A single object detected by the CV pipeline (e.g. YOLO).
 * {@code confidence} is in [0.0, 1.0].
 */
public record DetectedObject(String label, double confidence) {}

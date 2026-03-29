package com.roomtour.recognition.core.model;

import java.util.List;

/**
 * Webhook payload posted by the YOLO sidecar service.
 * Contains the objects it detected in the latest camera frame.
 */
public record SidecarPayload(List<DetectedObject> detectedObjects) {}

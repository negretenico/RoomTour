package com.roomtour.recognition.core.model;

/**
 * Depth-camera geometric snapshot of a room.
 * All distances are in metres; ratios are in [0.0, 1.0].
 */
public record GeometricSignature(
    double widthMeters,
    double lengthMeters,
    double ceilingHeightMeters,
    double openFloorRatio,
    int doorwayCount
) {}

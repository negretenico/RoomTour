package com.roomtour.assistant.core.model;

public record HealthData(
    double sleepHours,
    String sleepQuality,
    int stepsToday,
    int stepsGoal,
    String notes
) {}

package com.roomtour.assistant.core.model;

public record WeatherSnapshot(
    String location,
    String condition,
    double temperatureF,
    double highF,
    double lowF,
    String forecast
) {}

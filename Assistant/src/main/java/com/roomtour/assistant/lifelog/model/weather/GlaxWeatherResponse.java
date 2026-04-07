package com.roomtour.assistant.lifelog.model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GlaxWeatherResponse(
        String city,
        double temperature,
        String weather,
        int humidity,
        @JsonProperty("wind_speed") int windSpeed,
        @JsonProperty("temperature_unit") String temperatureUnit,
        String message
) {}

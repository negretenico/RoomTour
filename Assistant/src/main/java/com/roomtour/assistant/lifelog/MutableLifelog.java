package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.core.model.WeatherSnapshot;

public interface MutableLifelog {
    /** Replaces the current weather snapshot. Thread-safe. */
    void updateWeather(WeatherSnapshot snapshot);
}

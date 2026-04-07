package com.roomtour.assistant.lifelog;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.WeatherSnapshot;

public interface WeatherService {
    Try<WeatherSnapshot> fetchCurrent(String location);
}

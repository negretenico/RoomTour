package com.roomtour.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.config.GlaxWeatherProperties;
import com.roomtour.assistant.lifelog.GlaxWeatherService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WeatherTool implements ButlerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
        "weather_current",
        "Get the current weather conditions for a location. " +
        "Note: this source does not provide separate daily high/low values — do not report them.",
        Map.of("location", Map.of(
            "type", "string",
            "description", "City name or city and state, e.g. 'Austin, TX'"
        )),
        List.of("location")
    );

    private final GlaxWeatherService weatherService;
    private final GlaxWeatherProperties weatherProps;
    private final ObjectMapper objectMapper;

    public WeatherTool(GlaxWeatherService weatherService,
                       GlaxWeatherProperties weatherProps,
                       ObjectMapper objectMapper) {
        this.weatherService  = weatherService;
        this.weatherProps    = weatherProps;
        this.objectMapper    = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public String execute(Map<String, Object> input) {
        String location = (String) input.getOrDefault("location", weatherProps.location());
        return weatherService.fetchCurrent(location)
            .map(snapshot -> {
                try {
                    return objectMapper.writeValueAsString(snapshot);
                } catch (Exception e) {
                    return "{\"error\": \"Failed to serialize weather snapshot\"}";
                }
            })
            .getOrElse(() -> "{\"error\": \"Weather fetch failed\"}");
    }
}

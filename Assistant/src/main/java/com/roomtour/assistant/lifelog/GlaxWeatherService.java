package com.roomtour.assistant.lifelog;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.config.GlaxWeatherProperties;
import com.roomtour.assistant.core.model.WeatherSnapshot;
import com.roomtour.assistant.lifelog.model.weather.GlaxWeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class GlaxWeatherService implements WeatherService {

    private final WebClient          glaxWeatherWebClient;
    private final GlaxWeatherProperties props;

    public GlaxWeatherService(WebClient glaxWeatherWebClient, GlaxWeatherProperties props) {
        this.glaxWeatherWebClient = glaxWeatherWebClient;
        this.props                = props;
    }

    @Override
    public Try<WeatherSnapshot> fetchCurrent(String location) {
        return Try.of(() -> {
            GlaxWeatherResponse response = glaxWeatherWebClient.get()
                    .uri(u -> u
                            .path("/api/glax_weather.json")
                            .queryParam("location", location)
                            .queryParam("units", "imperial")
                            .build())
                    .retrieve()
                    .bodyToMono(GlaxWeatherResponse.class)
                    .block();

            return new WeatherSnapshot(
                    response.city(),
                    response.weather(),
                    response.temperature(),
                    response.temperature(),
                    response.temperature(),
                    response.message()
            );
        }).onFailure(e -> log.warn("GlaxWeather fetch failed: {}", e.getMessage(), e));
    }
}

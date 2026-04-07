package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.config.GlaxWeatherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LifelogRefreshScheduler {

    private final WeatherService            weatherService;
    private final ApplicationEventPublisher publisher;
    private final GlaxWeatherProperties     weatherProps;

    public LifelogRefreshScheduler(WeatherService weatherService,
                                   ApplicationEventPublisher publisher,
                                   GlaxWeatherProperties weatherProps) {
        this.weatherService = weatherService;
        this.publisher      = publisher;
        this.weatherProps   = weatherProps;
    }

    @Scheduled(fixedRateString = "${lifelog.refresh-rate-ms:900000}")
    public void refresh() {
        weatherService.fetchCurrent(weatherProps.location())
                .onFailure(e -> log.warn("Weather refresh failed: {}", e.getMessage()))
                .map(snapshot -> {
                    publisher.publishEvent(new WeatherRefreshedEvent(this, snapshot));
                    return snapshot;
                });
    }
}

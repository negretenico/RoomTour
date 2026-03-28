package com.roomtour.assistant.config;

import com.roomtour.assistant.core.model.CalendarEvent;
import com.roomtour.assistant.core.model.HealthData;
import com.roomtour.assistant.core.model.WeatherSnapshot;
import com.roomtour.assistant.lifelog.InMemoryLifelog;
import com.roomtour.assistant.lifelog.LifelogService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wires beans that can't use {@code @Service} directly.
 * {@link InMemoryLifelog} needs seed data converted from config → domain types,
 * so it lives here rather than as a stereotype.
 */
@Configuration
@EnableConfigurationProperties(ButlerProperties.class)
public class AssistantConfig {

    private final ButlerProperties props;

    public AssistantConfig(ButlerProperties props) {
        this.props = props;
    }

    @Bean
    public LifelogService lifelogService() {
        ButlerProperties.LifelogProperties lp = props.getLifelog();

        List<CalendarEvent> calendar = lp.getCalendar().stream()
            .map(e -> new CalendarEvent(e.getDate(), e.getTime(), e.getTitle()))
            .toList();

        ButlerProperties.WeatherConfig w = lp.getWeather();
        WeatherSnapshot weather = new WeatherSnapshot(
            w.getLocation(), w.getCondition(),
            w.getTemperatureF(), w.getHighF(), w.getLowF(), w.getForecast()
        );

        ButlerProperties.HealthConfig h = lp.getHealth();
        HealthData health = new HealthData(
            h.getSleepHours(), h.getSleepQuality(),
            h.getStepsToday(), h.getStepsGoal(), h.getNotes()
        );

        return new InMemoryLifelog(calendar, weather, health);
    }
}

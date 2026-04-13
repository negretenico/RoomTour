package com.roomtour.assistant.config;

import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CalendarEvent;
import com.roomtour.assistant.core.model.HealthData;
import com.roomtour.assistant.core.model.WeatherSnapshot;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.assistant.dispatch.PrefixCommandRouter;
import com.roomtour.assistant.dispatch.command.ButlerCommand;
import com.roomtour.assistant.dispatch.command.MapCommand;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.lifelog.InMemoryLifelog;
import com.roomtour.assistant.lifelog.LifelogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wires beans that can't use {@code @Service} directly.
 * {@link InMemoryLifelog} needs seed data converted from config → domain types,
 * so it lives here rather than as a stereotype.
 *
 * Registered as a Spring Boot auto-configuration so any application
 * that depends on roomtour-assistant gets all assistant beans automatically.
 */
@Configuration
@EnableConfigurationProperties({ButlerProperties.class, NavigationProperties.class})
@ComponentScan(basePackages = "com.roomtour.assistant")
public class AssistantConfig {

    private final ButlerProperties props;

    public AssistantConfig(ButlerProperties props) {
        this.props = props;
    }

    @Bean
    @ConditionalOnMissingBean(CurrentRoomRepository.class)
    public CurrentRoomRepository currentRoomRepository() {
        return sessionId -> "unknown";
    }

    @Bean
    @ConditionalOnMissingBean(LifelogService.class)
    public InMemoryLifelog lifelogService() {
        LifelogProperties lp = props.getLifelog();

        List<CalendarEvent> calendar = lp.getCalendar().stream()
            .map(e -> new CalendarEvent(e.getDate(), e.getTime(), e.getTitle()))
            .toList();

        WeatherConfig w = lp.getWeather();
        WeatherSnapshot weather = new WeatherSnapshot(
            w.getLocation(), w.getCondition(),
            w.getTemperatureF(), w.getHighF(), w.getLowF(), w.getForecast()
        );

        HealthConfig h = lp.getHealth();
        HealthData health = new HealthData(
            h.getSleepHours(), h.getSleepQuality(),
            h.getStepsToday(), h.getStepsGoal(), h.getNotes()
        );

        return new InMemoryLifelog(calendar, weather, health);
    }

    @Bean
    @ConditionalOnMissingBean(CommandRouter.class)
    public CommandRouter commandRouter(ChatService<ButlerResponse, ButlerRequest> chatService,
                                       List<ButlerCommand> commands,
                                       MapCommand mapCommand) {
        return new PrefixCommandRouter(chatService, commands, mapCommand);
    }
}

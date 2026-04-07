package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.core.model.WeatherSnapshot;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WeatherRefreshedEvent extends ApplicationEvent {

    private final WeatherSnapshot snapshot;

    public WeatherRefreshedEvent(Object source, WeatherSnapshot snapshot) {
        super(source);
        this.snapshot = snapshot;
    }
}

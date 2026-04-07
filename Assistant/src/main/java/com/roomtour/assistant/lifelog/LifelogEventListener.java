package com.roomtour.assistant.lifelog;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LifelogEventListener {

    private final MutableLifelog lifelog;

    public LifelogEventListener(MutableLifelog lifelog) {
        this.lifelog = lifelog;
    }

    @EventListener
    public void onWeatherRefreshed(WeatherRefreshedEvent event) {
        lifelog.updateWeather(event.getSnapshot());
    }
}

package com.roomtour.assistant.config;

import java.util.ArrayList;
import java.util.List;

public class LifelogProperties {

    private List<CalendarConfig> calendar = new ArrayList<>();
    private WeatherConfig        weather  = new WeatherConfig();
    private HealthConfig         health   = new HealthConfig();

    public List<CalendarConfig> getCalendar()       { return calendar; }
    public void setCalendar(List<CalendarConfig> c) { this.calendar = c; }

    public WeatherConfig getWeather()               { return weather; }
    public void setWeather(WeatherConfig w)         { this.weather = w; }

    public HealthConfig getHealth()                 { return health; }
    public void setHealth(HealthConfig h)           { this.health = h; }
}

package com.roomtour.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "butler")
public class ButlerProperties {

    private String name        = "Jeeves";
    private String personality = "formal and witty home drone butler";
    private String userName    = "you";
    private LifelogProperties lifelog = new LifelogProperties();

    // --- Getters / setters (Spring requires mutable JavaBeans for binding) ---

    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }

    public String getPersonality()               { return personality; }
    public void   setPersonality(String p)       { this.personality = p; }

    public String getUserName()                  { return userName; }
    public void   setUserName(String userName)   { this.userName = userName; }

    public LifelogProperties getLifelog()        { return lifelog; }
    public void setLifelog(LifelogProperties l)  { this.lifelog = l; }

    // -------------------------------------------------------------------------

    public static class LifelogProperties {
        private List<CalendarConfig> calendar = new ArrayList<>();
        private WeatherConfig        weather  = new WeatherConfig();
        private HealthConfig         health   = new HealthConfig();

        public List<CalendarConfig> getCalendar()         { return calendar; }
        public void setCalendar(List<CalendarConfig> c)   { this.calendar = c; }

        public WeatherConfig getWeather()                 { return weather; }
        public void setWeather(WeatherConfig w)           { this.weather = w; }

        public HealthConfig getHealth()                   { return health; }
        public void setHealth(HealthConfig h)             { this.health = h; }
    }

    public static class CalendarConfig {
        private String date;
        private String time;
        private String title;

        public String getDate()          { return date; }
        public void   setDate(String d)  { this.date = d; }
        public String getTime()          { return time; }
        public void   setTime(String t)  { this.time = t; }
        public String getTitle()         { return title; }
        public void   setTitle(String t) { this.title = t; }
    }

    public static class WeatherConfig {
        private String location    = "";
        private String condition   = "";
        private double temperatureF;
        private double highF;
        private double lowF;
        private String forecast    = "";

        public String getLocation()           { return location; }
        public void   setLocation(String l)   { this.location = l; }
        public String getCondition()          { return condition; }
        public void   setCondition(String c)  { this.condition = c; }
        public double getTemperatureF()       { return temperatureF; }
        public void   setTemperatureF(double t){ this.temperatureF = t; }
        public double getHighF()              { return highF; }
        public void   setHighF(double h)      { this.highF = h; }
        public double getLowF()               { return lowF; }
        public void   setLowF(double l)       { this.lowF = l; }
        public String getForecast()           { return forecast; }
        public void   setForecast(String f)   { this.forecast = f; }
    }

    public static class HealthConfig {
        private double sleepHours;
        private String sleepQuality = "";
        private int    stepsToday;
        private int    stepsGoal;
        private String notes        = "";

        public double getSleepHours()           { return sleepHours; }
        public void   setSleepHours(double h)   { this.sleepHours = h; }
        public String getSleepQuality()         { return sleepQuality; }
        public void   setSleepQuality(String q) { this.sleepQuality = q; }
        public int    getStepsToday()           { return stepsToday; }
        public void   setStepsToday(int s)      { this.stepsToday = s; }
        public int    getStepsGoal()            { return stepsGoal; }
        public void   setStepsGoal(int g)       { this.stepsGoal = g; }
        public String getNotes()                { return notes; }
        public void   setNotes(String n)        { this.notes = n; }
    }
}

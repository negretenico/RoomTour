package com.roomtour.assistant.config;

public class WeatherConfig {

    private String location     = "";
    private String condition    = "";
    private double temperatureF;
    private double highF;
    private double lowF;
    private String forecast     = "";

    public String getLocation()            { return location; }
    public void   setLocation(String l)    { this.location = l; }

    public String getCondition()           { return condition; }
    public void   setCondition(String c)   { this.condition = c; }

    public double getTemperatureF()        { return temperatureF; }
    public void   setTemperatureF(double t){ this.temperatureF = t; }

    public double getHighF()               { return highF; }
    public void   setHighF(double h)       { this.highF = h; }

    public double getLowF()                { return lowF; }
    public void   setLowF(double l)        { this.lowF = l; }

    public String getForecast()            { return forecast; }
    public void   setForecast(String f)    { this.forecast = f; }
}

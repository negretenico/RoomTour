package com.roomtour.assistant.config;

public class HealthConfig {

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

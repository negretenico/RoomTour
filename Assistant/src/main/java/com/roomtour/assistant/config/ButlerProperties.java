package com.roomtour.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "butler")
public class ButlerProperties {

    private String           name        = "Jeeves";
    private String           personality = "formal and witty home drone butler";
    private String           userName    = "you";
    private LifelogProperties lifelog    = new LifelogProperties();

    public String getName()                          { return name; }
    public void   setName(String name)               { this.name = name; }

    public String getPersonality()                   { return personality; }
    public void   setPersonality(String p)           { this.personality = p; }

    public String getUserName()                      { return userName; }
    public void   setUserName(String userName)       { this.userName = userName; }

    public LifelogProperties getLifelog()            { return lifelog; }
    public void setLifelog(LifelogProperties l)      { this.lifelog = l; }
}

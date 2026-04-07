package com.roomtour.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "glax-weather")
public record GlaxWeatherProperties(
        @DefaultValue("") String location,
        @DefaultValue("https://dragon.best") String baseUrl
) {}

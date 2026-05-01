package com.roomtour.assistant.config;

import com.roomtour.assistant.briefing.BriefingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({GlaxWeatherProperties.class, BriefingProperties.class})
public class SchedulingConfig {
}

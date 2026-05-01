package com.roomtour.assistant.briefing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "butler.briefings")
public record BriefingProperties(
        @DefaultValue("0 0 8 * * *")  String morningCron,
        @DefaultValue("0 0 18 * * *") String eveningCron
) {}

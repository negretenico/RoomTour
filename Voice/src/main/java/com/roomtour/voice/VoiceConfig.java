package com.roomtour.voice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration entry point for the Voice module.
 * Registered in META-INF/spring/AutoConfiguration.imports so any Spring Boot
 * application that depends on roomtour-voice picks up all voice beans automatically.
 */
@Configuration
@EnableConfigurationProperties(VoiceProperties.class)
@ComponentScan(basePackages = "com.roomtour.voice")
public class VoiceConfig {

    @Bean("whisperWebClient")
    @ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
    public WebClient whisperWebClient(VoiceProperties props) {
        return WebClient.builder()
                .baseUrl(props.whisperUrl())
                .build();
    }
}

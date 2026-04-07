package com.roomtour.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("glaxWeatherWebClient")
    public WebClient glaxWeatherWebClient(GlaxWeatherProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl() + "/api/glax_weather.json")
                .build();
    }
}

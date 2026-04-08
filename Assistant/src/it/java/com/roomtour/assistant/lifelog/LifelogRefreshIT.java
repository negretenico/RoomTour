package com.roomtour.assistant.lifelog;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.config.AssistantConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest(classes = AssistantConfig.class)
@ActiveProfiles("it")
class LifelogRefreshIT {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureBaseUrls(DynamicPropertyRegistry registry) {
        registry.add("glax-weather.base-url", wireMock::baseUrl);
    }

    @Autowired LifelogService lifelogService;

    @BeforeEach
    void stubExternalApis() {
        wireMock.stubFor(get(urlPathEqualTo("/api/glax_weather.json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "city": "Austin",
                                  "temperature": 82.0,
                                  "weather": "clear sky",
                                  "humidity": 45,
                                  "wind_speed": 10,
                                  "temperature_unit": "F",
                                  "message": "Weather in Austin is 82.0 F, 45% Humidity, clear sky"
                                }
                                """)));
    }

    @Test
    void schedulerPopulatesWeatherIntoLifelog() {
        await().atMost(3, SECONDS).untilAsserted(() -> {
            String prompt = lifelogService.formatForPrompt();
            assertThat(prompt).contains("Austin");
            assertThat(prompt).contains("clear sky");
        });
    }
}

package com.roomtour.assistant.ai;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.roomtour.assistant.chat.ButlerChatService;
import com.roomtour.assistant.config.AssistantConfig;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full agentic tool-use loop end-to-end:
 * Claude returns a {@code weather_current} tool_use block, the loop executes
 * GlaxWeatherService, sends the result back, and Claude returns the final text.
 *
 * WireMock intercepts both the Anthropic API and the GlaxWeather API so no
 * real network calls are made.
 */
@SpringBootTest(classes = AssistantConfig.class)
@ActiveProfiles("prod")
class AgenticWeatherIT {

    private static final String SCENARIO          = "weather-tool-use";
    private static final String STATE_TOOL_CALLED = "tool-result-sent";

    @RegisterExtension
    static WireMockExtension anthropicMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @RegisterExtension
    static WireMockExtension glaxWeatherMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void configureUrls(DynamicPropertyRegistry registry) {
        registry.add("anthropic.base-url",   anthropicMock::baseUrl);
        registry.add("anthropic.api-key",    () -> "test-key");
        registry.add("glax-weather.base-url", glaxWeatherMock::baseUrl);
    }

    @Autowired ButlerChatService butlerChatService;

    @BeforeEach
    void stubApis() {
        glaxWeatherMock.stubFor(get(urlPathEqualTo("/api/glax_weather.json"))
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
                      "message": "Weather in Austin is 82.0 F, clear sky"
                    }
                    """)));

        // First Anthropic call → Claude requests the weather tool
        anthropicMock.stubFor(post(urlPathEqualTo("/v1/messages"))
            .inScenario(SCENARIO)
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": "msg_tool_use",
                      "type": "message",
                      "role": "assistant",
                      "content": [
                        {
                          "type": "tool_use",
                          "id": "toolu_weather_01",
                          "name": "weather_current",
                          "input": {"location": "Austin, TX"}
                        }
                      ],
                      "model": "claude-3-5-haiku-20241022",
                      "stop_reason": "tool_use",
                      "stop_sequence": null,
                      "usage": {"input_tokens": 100, "output_tokens": 50}
                    }
                    """))
            .willSetStateTo(STATE_TOOL_CALLED));

        // Second Anthropic call (with tool result) → Claude returns final text
        anthropicMock.stubFor(post(urlPathEqualTo("/v1/messages"))
            .inScenario(SCENARIO)
            .whenScenarioStateIs(STATE_TOOL_CALLED)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": "msg_final_text",
                      "type": "message",
                      "role": "assistant",
                      "content": [
                        {
                          "type": "text",
                          "text": "It's currently 82°F in Austin with clear skies."
                        }
                      ],
                      "model": "claude-3-5-haiku-20241022",
                      "stop_reason": "end_turn",
                      "stop_sequence": null,
                      "usage": {"input_tokens": 150, "output_tokens": 40}
                    }
                    """)));
    }

    @Test
    void weatherQuestionExecutesToolLoopAndReturnsWeatherContent() {
        ButlerResponse response = butlerChatService.chat(
            new ButlerRequest("What's the weather like today?", "living room", "agentic-it-session")
        );

        assertThat(response.response()).containsIgnoringCase("Austin");
        assertThat(response.response()).containsIgnoringCase("clear");
    }

    @Test
    void anthropicEndpointIsCalledTwice() {
        butlerChatService.chat(
            new ButlerRequest("How's the weather?", "kitchen", "agentic-it-session-2")
        );

        // Anthropic is called exactly twice: once for tool_use, once with the tool result.
        anthropicMock.verify(2, postRequestedFor(urlPathEqualTo("/v1/messages")));
        // GlaxWeather is called at least once by the tool; the scheduler may add extra calls.
        glaxWeatherMock.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo("/api/glax_weather.json")));
    }
}

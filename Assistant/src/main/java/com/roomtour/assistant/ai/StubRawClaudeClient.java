package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.config.GlaxWeatherProperties;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.tools.ToolDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local-profile raw client that simulates a Claude tool-use sequence without
 * hitting the Anthropic API.  First call returns a {@code weather_current} tool
 * use; subsequent calls (once agent turns exist) return the canned text stub.
 */
@Component
@Profile("local")
public class StubRawClaudeClient implements RawClaudeClient {

    private final String                cannedText;
    private final GlaxWeatherProperties weatherProps;

    public StubRawClaudeClient(ObjectMapper objectMapper,
                                GlaxWeatherProperties weatherProps) throws Exception {
        var resource = new ClassPathResource("stubs/claude-stub-response.json");
        JsonNode root = objectMapper.readTree(resource.getInputStream());
        this.cannedText   = root.get("response").asText();
        this.weatherProps = weatherProps;
    }

    @Override
    public Try<RawResponse> send(String systemPrompt, List<Message> history,
                                 List<AgentTurn> agentTurns, List<ToolDefinition> tools) {
        if (agentTurns.isEmpty()) {
            return Try.of(() -> new RawResponse.ToolUse(List.of(
                new ToolCall(
                    UUID.randomUUID().toString(),
                    "weather_current",
                    Map.of("location", weatherProps.location())
                )
            )));
        }
        return Try.of(() -> new RawResponse.Text(cannedText));
    }
}

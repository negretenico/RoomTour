package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.core.model.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Local-profile stub — reads a canned response from {@code stubs/claude-stub-response.json}.
 * Activated by {@code spring.profiles.active=local}; never active in prod.
 */
@Component
@Profile({"local", "it"})
public class StubClaudeClient implements ClaudeClient {

    private final String cannedResponse;

    public StubClaudeClient(ObjectMapper objectMapper) throws Exception {
        var resource = new ClassPathResource("stubs/claude-stub-response.json");
        JsonNode root = objectMapper.readTree(resource.getInputStream());
        this.cannedResponse = root.get("response").asText();
    }

    @Override
    public Try<String> send(String systemPrompt, List<Message> history) {
        return Try.of(() -> cannedResponse);
    }
}

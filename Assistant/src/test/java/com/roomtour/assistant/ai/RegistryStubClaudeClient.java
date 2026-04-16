package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;

import java.util.List;
import java.util.Map;

/**
 * Test-only stub that returns different canned responses based on the last user message.
 * Construct with a default response and an optional registry map.
 * Utterances not in the registry fall back to the default.
 */
public class RegistryStubClaudeClient implements ClaudeClient {

    private final String defaultResponse;
    private final Map<String, String> registry;

    public RegistryStubClaudeClient(String defaultResponse) {
        this(defaultResponse, Map.of());
    }

    public RegistryStubClaudeClient(String defaultResponse, Map<String, String> registry) {
        this.defaultResponse = defaultResponse;
        this.registry        = registry;
    }

    @Override
    public Try<String> send(String systemPrompt, List<Message> history) {
        String lastUserMessage = history.stream()
            .filter(m -> "user".equals(m.role()))
            .reduce((a, b) -> b)
            .map(Message::content)
            .orElse("");
        String response = registry.getOrDefault(lastUserMessage, defaultResponse);
        return Try.of(() -> response);
    }
}

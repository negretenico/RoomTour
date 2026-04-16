package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only stub that returns different canned responses based on the last user message.
 * Register utterance → response pairs before the test runs. Unregistered utterances fall
 * back to the default response.
 *
 * <pre>{@code
 * var stub = new RegistryStubClaudeClient("Good day, sir.")
 *     .register("Hello", "Good morning!")
 *     .register("what's the weather", "Sunny, 72°F.");
 * }</pre>
 */
public class RegistryStubClaudeClient implements ClaudeClient {

    private final String defaultResponse;
    private final Map<String, String> registry = new HashMap<>();

    public RegistryStubClaudeClient(String defaultResponse) {
        this.defaultResponse = defaultResponse;
    }

    public RegistryStubClaudeClient register(String utterance, String response) {
        registry.put(utterance, response);
        return this;
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

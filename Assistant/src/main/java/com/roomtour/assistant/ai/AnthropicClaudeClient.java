package com.roomtour.assistant.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wraps the Anthropic Java SDK. Reads ANTHROPIC_API_KEY from the environment.
 * All domain code talks to {@link ClaudeClient} — this class is the only place
 * that imports the SDK.
 */
@Component
public class AnthropicClaudeClient implements ClaudeClient {

    private static final String MODEL      = Model.CLAUDE_3_5_HAIKU_LATEST.asString();
    private static final long   MAX_TOKENS = 1024L;

    private final AnthropicClient client;

    public AnthropicClaudeClient() {
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    @Override
    public Try<String> send(String systemPrompt, List<Message> history) {
        return Try.of(() -> {
            var response = client.messages().create(buildParams(systemPrompt, history));
            return response.content().stream()
                .filter(ContentBlock::isText)
                .map(b -> b.asText().text())
                .findFirst()
                .orElse("");
        });
    }

    private MessageCreateParams buildParams(String systemPrompt, List<Message> history) {
        var builder = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(MAX_TOKENS)
            .system(systemPrompt);

        history.forEach(m -> {
            if ("user".equals(m.role())) {
                builder.addUserMessage(m.content());
            } else {
                builder.addAssistantMessage(m.content());
            }
        });

        return builder.build();
    }
}

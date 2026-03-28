package com.roomtour.assistant.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wraps the Anthropic Java SDK. Reads ANTHROPIC_API_KEY from the environment.
 * All domain code talks to {@link ClaudeClient} — this class is the only place
 * that imports the SDK.
 */
@Slf4j
@Component
@Profile("prod")
public class AnthropicClaudeClient implements ClaudeClient {

    private static final String MODEL      = Model.CLAUDE_3_5_HAIKU_LATEST.asString();
    private static final long   MAX_TOKENS = 1024L;

    private final AnthropicClient client;

    public AnthropicClaudeClient() {
        this.client = AnthropicOkHttpClient.fromEnv();
        log.info("AnthropicClaudeClient initialized with model={}", MODEL);
    }

    @Override
    public Try<String> send(String systemPrompt, List<Message> history) {
        log.debug("Sending request to Anthropic: historySize={}", history.size());
        return Try.of(() -> {
            var response = client.messages().create(buildParams(systemPrompt, history));
            String text = response.content().stream()
                .filter(ContentBlock::isText)
                .map(b -> b.asText().text())
                .findFirst()
                .orElse("");
            log.debug("Received response from Anthropic: length={}", text.length());
            return text;
        }).onFailure(e -> log.error("Anthropic API call failed: {}", e.getMessage(), e));
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

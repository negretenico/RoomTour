package com.roomtour.assistant.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;
import com.common.functionico.risky.Try;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.tools.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps the Anthropic Java SDK for a single message turn.
 * Returns either a text response or a list of tool calls — the agentic loop
 * is handled by {@link AgenticClaudeClient}, not here.
 */
@Slf4j
@Component
@Profile("prod")
public class AnthropicRawClaudeClient implements RawClaudeClient {

    private static final String MODEL      = Model.CLAUDE_3_5_HAIKU_LATEST.asString();
    private static final long   MAX_TOKENS = 1024L;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AnthropicClient client;
    private final ObjectMapper    objectMapper;

    public AnthropicRawClaudeClient(
            ObjectMapper objectMapper,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${anthropic.api-key:}") String apiKeyProp) {
        String apiKey = apiKeyProp.isBlank()
            ? Optional.ofNullable(System.getenv("ANTHROPIC_API_KEY")).orElse("")
            : apiKeyProp;
        this.client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        this.objectMapper = objectMapper;
        log.info("AnthropicRawClaudeClient initialized with model={}, baseUrl={}", MODEL, baseUrl);
    }

    @Override
    public Try<RawResponse> send(String systemPrompt, List<Message> history,
                                 List<AgentTurn> agentTurns, List<ToolDefinition> toolDefs) {
        log.debug("Sending raw request: historySize={}, agentTurns={}", history.size(), agentTurns.size());
        return Try.of(() -> {
            var builder = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .system(systemPrompt);

            history.forEach(m -> {
                if ("user".equals(m.role())) builder.addUserMessage(m.content());
                else                          builder.addAssistantMessage(m.content());
            });

            agentTurns.forEach(turn -> {
                builder.addAssistantMessageOfBlockParams(
                    turn.calls().stream()
                        .map(call -> ContentBlockParam.ofToolUse(
                            ToolUseBlockParam.builder()
                                .id(call.id())
                                .name(call.name())
                                .input(JsonValue.from(call.input()))
                                .build()))
                        .toList());

                builder.addUserMessageOfBlockParams(
                    turn.results().stream()
                        .map(result -> ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                .toolUseId(result.toolCallId())
                                .content(result.content())
                                .build()))
                        .toList());
            });

            toolDefs.forEach(def -> builder.addTool(buildTool(def)));

            var response = client.messages().create(builder.build());

            List<ToolCall> toolCalls = response.content().stream()
                .filter(ContentBlock::isToolUse)
                .map(block -> {
                    var tu = block.asToolUse();
                    Map<String, Object> input = objectMapper.convertValue(tu._input(), MAP_TYPE);
                    return new ToolCall(tu.id(), tu.name(), input);
                })
                .toList();

            RawResponse rawResponse;
            if (!toolCalls.isEmpty()) {
                log.debug("Claude requested {} tool call(s)", toolCalls.size());
                rawResponse = new RawResponse.ToolUse(toolCalls);
            } else {
                String text = response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(b -> b.asText().text())
                    .findFirst()
                    .orElse("");
                log.debug("Claude returned text response: length={}", text.length());
                rawResponse = new RawResponse.Text(text);
            }
            return rawResponse;
        }).onFailure(e -> log.error("Anthropic raw API call failed: {}", e.getMessage(), e));
    }

    private Tool buildTool(ToolDefinition def) {
        return Tool.builder()
            .name(def.name())
            .description(def.description())
            .inputSchema(Tool.InputSchema.builder()
                .putAdditionalProperty("type",       JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(def.inputProperties()))
                .putAdditionalProperty("required",   JsonValue.from(def.required()))
                .build())
            .build();
    }
}

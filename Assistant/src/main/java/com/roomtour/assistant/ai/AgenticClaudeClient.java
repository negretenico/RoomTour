package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the agentic tool-use loop on top of any {@link RawClaudeClient}.
 * Callers see only {@link ClaudeClient} — tool exchanges are an API-level concern.
 */
@Slf4j
@Component
@Profile({"prod", "local"})
public class AgenticClaudeClient implements ClaudeClient {

    static final int MAX_ITERATIONS = 5;

    private final RawClaudeClient rawClient;
    private final ToolRegistry    toolRegistry;

    public AgenticClaudeClient(RawClaudeClient rawClient, ToolRegistry toolRegistry) {
        this.rawClient    = rawClient;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Try<String> send(String systemPrompt, List<Message> history) {
        return Try.of(() -> runLoop(systemPrompt, history));
    }

    private String runLoop(String systemPrompt, List<Message> history) throws Exception {
        List<AgentTurn> agentTurns = new ArrayList<>();
        var toolDefs = toolRegistry.definitions();

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            RawResponse response;
            try {
                response = rawClient.send(systemPrompt, history, agentTurns, toolDefs).getOrThrow();
            } catch (Throwable t) {
                throw new RuntimeException("Raw Claude call failed", t);
            }

            if (response instanceof RawResponse.Text text) {
                return text.content();
            }

            if (response instanceof RawResponse.ToolUse toolUse) {
                log.debug("Executing {} tool call(s) on iteration {}", toolUse.calls().size(), i + 1);
                List<ToolCallResult> results = toolUse.calls().stream()
                    .map(call -> new ToolCallResult(
                        call.id(),
                        toolRegistry.execute(call.name(), call.input())
                    ))
                    .toList();
                agentTurns.add(new AgentTurn(toolUse.calls(), results));
            }
        }

        log.warn("Agentic loop hit max iterations ({})", MAX_ITERATIONS);
        throw new IllegalStateException("Agentic loop exceeded max iterations: " + MAX_ITERATIONS);
    }
}

package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.tools.ToolDefinition;

import java.util.List;

public interface RawClaudeClient {
    Try<RawResponse> send(String systemPrompt, List<Message> history,
                          List<AgentTurn> agentTurns, List<ToolDefinition> tools);
}

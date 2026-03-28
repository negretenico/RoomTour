package com.roomtour.assistant.ai;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.Message;

import java.util.List;

public interface ClaudeClient {
    Try<String> send(String systemPrompt, List<Message> history);
}

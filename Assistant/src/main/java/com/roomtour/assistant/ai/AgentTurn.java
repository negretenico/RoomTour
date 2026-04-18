package com.roomtour.assistant.ai;

import java.util.List;

public record AgentTurn(List<ToolCall> calls, List<ToolCallResult> results) {}

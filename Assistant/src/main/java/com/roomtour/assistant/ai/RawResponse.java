package com.roomtour.assistant.ai;

import java.util.List;

public sealed interface RawResponse {
    record Text(String content) implements RawResponse {}
    record ToolUse(List<ToolCall> calls) implements RawResponse {}
}

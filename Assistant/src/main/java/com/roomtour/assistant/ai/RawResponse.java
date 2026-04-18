package com.roomtour.assistant.ai;

import java.util.List;

public sealed interface RawResponse permits RawResponse.Text, RawResponse.ToolUse {
    record Text(String content) implements RawResponse {}
    record ToolUse(List<ToolCall> calls) implements RawResponse {}
}

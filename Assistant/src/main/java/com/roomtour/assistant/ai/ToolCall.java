package com.roomtour.assistant.ai;

import java.util.Map;

public record ToolCall(String id, String name, Map<String, Object> input) {}

package com.roomtour.assistant.tools;

import java.util.List;
import java.util.Map;

public record ToolDefinition(
    String name,
    String description,
    Map<String, Object> inputProperties,
    List<String> required
) {}

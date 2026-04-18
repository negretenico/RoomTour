package com.roomtour.assistant.tools;

import java.util.Map;

public interface ButlerTool {
    ToolDefinition definition();
    String execute(Map<String, Object> input);
}

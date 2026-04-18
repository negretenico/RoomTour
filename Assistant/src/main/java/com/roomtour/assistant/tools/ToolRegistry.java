package com.roomtour.assistant.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final List<ButlerTool> tools;

    public ToolRegistry(List<ButlerTool> tools) {
        this.tools = tools;
    }

    public List<ToolDefinition> definitions() {
        return tools.stream().map(ButlerTool::definition).toList();
    }

    public String execute(String name, Map<String, Object> input) {
        return tools.stream()
            .filter(t -> t.definition().name().equals(name))
            .findFirst()
            .map(t -> t.execute(input))
            .orElse("{\"error\": \"Unknown tool: " + name + "\"}");
    }
}

package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import org.springframework.stereotype.Component;

@Component
public class WhatsNewCommand implements ButlerCommand {

    private final LifelogService lifelogService;

    public WhatsNewCommand(LifelogService lifelogService) {
        this.lifelogService = lifelogService;
    }

    @Override public String token() { return "/whats-new"; }
    @Override public String usage() { return "/whats-new"; }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String context = lifelogService.formatForPrompt();
        String response = context.isBlank()
            ? "Nothing new in your lifelog."
            : "Here's what's going on:\n" + context;
        return new ButlerResponse(response, sessionId);
    }
}

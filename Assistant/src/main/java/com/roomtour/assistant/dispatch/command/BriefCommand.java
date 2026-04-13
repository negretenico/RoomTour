package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BriefCommand implements ButlerCommand {

    private final LifelogService lifelogService;
    private final ClaudeClient claudeClient;
    private final ButlerProperties butlerProps;

    public BriefCommand(LifelogService lifelogService,
                        ClaudeClient claudeClient,
                        ButlerProperties butlerProps) {
        this.lifelogService = lifelogService;
        this.claudeClient   = claudeClient;
        this.butlerProps    = butlerProps;
    }

    @Override public String token() { return "/brief"; }
    @Override public String usage() { return "/brief"; }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String prompt = String.format(
            "You are %s, %s's home drone butler. Give a concise daily brief based on this context:%n%s",
            butlerProps.getName(), butlerProps.getUserName(), lifelogService.formatForPrompt()
        );
        return claudeClient.send(prompt, List.of())
            .map(text -> new ButlerResponse(text, sessionId))
            .getOrElse(() -> new ButlerResponse("Unable to generate brief right now.", sessionId));
    }
}

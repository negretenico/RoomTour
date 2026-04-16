package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WhatsNewCommand implements ButlerCommand {

    private static final Pattern INTENT = Pattern.compile(
        "(?i)(what'?s\\s+new|whats\\s+new|any\\s+news|what'?s\\s+happening|what'?s\\s+going\\s+on|weather|forecast)"
    );

    private final LifelogService lifelogService;

    public WhatsNewCommand(LifelogService lifelogService) {
        this.lifelogService = lifelogService;
    }

    @Override public String token() { return "/whats-new"; }
    @Override public String usage() { return "/whats-new"; }
    @Override public Optional<Pattern> intentPattern() { return Optional.of(INTENT); }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String context = lifelogService.formatForPrompt();
        String response = context.isBlank()
            ? "Nothing new in your lifelog."
            : "Here's what's going on:\n" + context;
        log.info("[WHATS-NEW] session={}", sessionId);
        return new ButlerResponse(response, sessionId);
    }
}

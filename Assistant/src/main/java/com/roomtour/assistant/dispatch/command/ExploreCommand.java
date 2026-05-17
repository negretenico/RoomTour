package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.navigation.ExplorationService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ExploreCommand implements ButlerCommand {

    private static final Pattern INTENT = Pattern.compile(
        "(?i)\\b(start\\s+explor\\w*|begin\\s+explor\\w*|explor\\w*\\s+the\\s+home|stop\\s+explor\\w*|end\\s+explor\\w*)\\b"
    );

    private final ExplorationService explorationService;

    public ExploreCommand(ExplorationService explorationService) {
        this.explorationService = explorationService;
    }

    @Override public String token() { return "/explore"; }
    @Override public String usage() { return "/explore [stop]"; }
    @Override public Optional<Pattern> intentPattern() { return Optional.of(INTENT); }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String args = message.substring(token().length()).strip();
        if ("stop".equalsIgnoreCase(args)) {
            return explorationService.stop(sessionId);
        }
        return explorationService.start(sessionId);
    }

    @Override
    public ButlerResponse intentExecute(String rawMessage, String sessionId) {
        String lower = rawMessage.toLowerCase();
        if (lower.contains("stop") || lower.contains("end")) {
            return explorationService.stop(sessionId);
        }
        return explorationService.start(sessionId);
    }
}

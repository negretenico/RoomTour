package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;

import java.util.Optional;
import java.util.regex.Pattern;

public interface ButlerCommand {

    /** The slash token this command handles, e.g. {@code "/navigate"}. */
    String token();

    /** One-line description shown by {@code /commands}, e.g. {@code "/navigate <room>"}. */
    String usage();

    /** Execute the command. {@code message} is the full raw input including the token. */
    ButlerResponse execute(String message, String sessionId);

    /**
     * Optional regex pattern that matches natural-language utterances for this command.
     * When present, the router calls {@link #intentExecute(String, String)} with the raw utterance.
     */
    default Optional<Pattern> intentPattern() {
        return Optional.empty();
    }

    /**
     * Executes this command from a natural-language utterance.
     * The default strips the utterance and delegates using only the token (no args).
     * Commands that need to forward content from the utterance (e.g. {@code MapCommand})
     * should override this.
     */
    default ButlerResponse intentExecute(String rawMessage, String sessionId) {
        return execute(token(), sessionId);
    }
}

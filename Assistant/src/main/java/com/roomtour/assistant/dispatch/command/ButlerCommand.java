package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;

public interface ButlerCommand {

    /** The slash token this command handles, e.g. {@code "/navigate"}. */
    String token();

    /** One-line description shown by {@code /commands}, e.g. {@code "/navigate <room>"}. */
    String usage();

    /** Execute the command. {@code message} is the full raw input including the token. */
    ButlerResponse execute(String message, String sessionId);
}

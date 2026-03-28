package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;

/** Single entry point for all butler interactions. Commands take precedence over conversation. */
public interface CommandRouter {
    ButlerResponse route(ButlerRequest request);
}

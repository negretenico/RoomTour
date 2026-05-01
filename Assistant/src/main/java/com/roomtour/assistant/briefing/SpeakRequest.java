package com.roomtour.assistant.briefing;

import org.springframework.context.ApplicationEvent;

/**
 * Requests that the voice layer speak a given text.
 * Decouples assistant-layer producers (e.g. BriefingScheduler) from
 * the voice module's TTS + audio playback infrastructure.
 */
public class SpeakRequest extends ApplicationEvent {

    private final String text;

    public SpeakRequest(Object source, String text) {
        super(source);
        this.text = text;
    }

    public String text() {
        return text;
    }
}

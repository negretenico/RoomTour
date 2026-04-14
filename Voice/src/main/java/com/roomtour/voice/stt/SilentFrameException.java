package com.roomtour.voice.stt;

/**
 * Signals that an audio chunk was below the speech threshold or a Whisper hallucination.
 * Not an error — the voice loop should silently skip the current iteration.
 */
public class SilentFrameException extends RuntimeException {
    public SilentFrameException(String reason) {
        super(reason);
    }
}

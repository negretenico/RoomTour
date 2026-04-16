package com.roomtour.assistant.dispatch;

/**
 * Utility for normalising raw Whisper transcripts before routing.
 * Whisper frequently appends punctuation (periods, commas) to otherwise
 * bare phrases — stripping it prevents false parse failures downstream.
 */
public final class TranscriptCleaner {

    private TranscriptCleaner() {}

    /** Strips leading and trailing whitespace and punctuation from a transcript fragment. */
    public static String stripPunctuation(String s) {
        return s.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").strip();
    }
}

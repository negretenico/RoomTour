package com.roomtour.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "butler.voice")
public record VoiceProperties(
        @DefaultValue("false")                    boolean enabled,
        @DefaultValue("http://localhost:8000")    String  whisperUrl,
        @DefaultValue("espeak-ng")                String  espeakBinary,
        @DefaultValue("800")                      int     silenceDurationMs,
        @DefaultValue("200")                      int     silenceThreshold,
        @DefaultValue("15000")                    int     maxDurationMs,
        @DefaultValue("16000")                    int     sampleRateHz
) {}

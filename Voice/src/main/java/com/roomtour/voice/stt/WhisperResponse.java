package com.roomtour.voice.stt;

import java.util.List;

record WhisperResponse(String text, List<WhisperSegment> segments) {

    double avgLogprob() {
        if (segments == null || segments.isEmpty()) return 0.0;
        return segments.stream()
                .mapToDouble(WhisperSegment::avgLogprob)
                .average()
                .orElse(0.0);
    }
}

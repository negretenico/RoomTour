package com.roomtour.voice.stt;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;

public interface SpeechToText {
    Try<String> transcribe(AudioChunk chunk);
}

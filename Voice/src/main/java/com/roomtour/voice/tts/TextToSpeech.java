package com.roomtour.voice.tts;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;

public interface TextToSpeech {
    Try<AudioChunk> synthesize(String text);
}

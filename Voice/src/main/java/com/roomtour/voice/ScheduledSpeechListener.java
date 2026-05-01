package com.roomtour.voice;

import com.roomtour.assistant.briefing.SpeakRequest;
import com.roomtour.voice.tts.AudioPlayer;
import com.roomtour.voice.tts.TextToSpeech;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link SpeakRequest} events published by assistant-layer components
 * (e.g. BriefingScheduler) and routes them through TTS + audio playback.
 *
 * Keeps the assistant module decoupled from voice infrastructure.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class ScheduledSpeechListener {

    private final TextToSpeech tts;
    private final AudioPlayer  audioPlayer;

    public ScheduledSpeechListener(TextToSpeech tts, AudioPlayer audioPlayer) {
        this.tts         = tts;
        this.audioPlayer = audioPlayer;
    }

    @EventListener
    public void onSpeakRequest(SpeakRequest event) {
        log.info("[Speech] Speaking scheduled message: \"{}\"", event.text());
        tts.synthesize(event.text())
           .onSuccess(audioPlayer::play)
           .onFailure(e -> log.warn("[Speech] TTS failed: {}", e.getMessage()));
    }
}

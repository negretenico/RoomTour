package com.roomtour.voice;

import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.voice.stt.MicCapture;
import com.roomtour.voice.stt.SilentFrameException;
import com.roomtour.voice.stt.SpeechToText;
import com.roomtour.voice.tts.AudioPlayer;
import com.roomtour.voice.tts.TextToSpeech;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Drives the full voice pipeline as a background thread:
 * mic → STT → CommandRouter → TTS → speaker.
 * Only active when butler.voice.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class VoiceLoop {

    private final MicCapture            micCapture;
    private final SpeechToText          stt;
    private final CommandRouter         commandRouter;
    private final TextToSpeech          tts;
    private final AudioPlayer           audioPlayer;
    private final CurrentRoomRepository roomRepository;

    private final String sessionId = UUID.randomUUID().toString();

    private volatile boolean running = true;
    private Thread thread;

    public VoiceLoop(MicCapture micCapture,
                     SpeechToText stt,
                     CommandRouter commandRouter,
                     TextToSpeech tts,
                     AudioPlayer audioPlayer,
                     CurrentRoomRepository roomRepository) {
        this.micCapture     = micCapture;
        this.stt            = stt;
        this.commandRouter  = commandRouter;
        this.tts            = tts;
        this.audioPlayer    = audioPlayer;
        this.roomRepository = roomRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("VoiceLoop starting — session {}", sessionId);
        thread = new Thread(this::loop, "voice-loop");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void stop() {
        log.info("VoiceLoop stopping");
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void loop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            micCapture.capture()
                    .onSuccess(chunk -> log.info("[STT] Captured {} bytes, sending to Whisper", chunk.pcm().length))
                    .flatMap(stt::transcribe)
                    .onSuccess(transcript -> log.info("[STT] Transcribed: \"{}\"", transcript))
                    .map(transcript -> commandRouter.route(
                            new ButlerRequest(transcript, roomRepository.getCurrentRoom(sessionId), sessionId)))
                    .onSuccess(response -> log.info("[Brain] Response: \"{}\"", response.response()))
                    .map(ButlerResponse::response)
                    .flatMap(tts::synthesize)
                    .onSuccess(chunk -> log.info("[TTS] Synthesized {} bytes, playing", chunk.pcm().length))
                    .onSuccess(audioPlayer::play)
                    .onFailure(e -> {
                        if (e instanceof SilentFrameException) log.debug("Silent frame — skipping TTS");
                        else log.warn("Voice loop iteration failed: {}", e.getMessage());
                    });
        }
        log.info("VoiceLoop exited");
    }
}

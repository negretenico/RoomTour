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
 * Drives the full voice pipeline as a background thread.
 *
 * State machine:
 *  STANDBY — lightweight loop; listens for the wake phrase via short Whisper clips.
 *  ACTIVE  — full pipeline: mic → STT → CommandRouter → TTS → speaker.
 *            Returns to STANDBY on sleep phrase or inactivity timeout.
 *
 * Only active when butler.voice.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class VoiceLoop {

    enum State { STANDBY, ACTIVE }

    private final MicCapture            micCapture;
    private final SpeechToText          stt;
    private final CommandRouter         commandRouter;
    private final TextToSpeech          tts;
    private final AudioPlayer           audioPlayer;
    private final CurrentRoomRepository roomRepository;
    private final VoiceProperties       props;

    private final String sessionId = UUID.randomUUID().toString();

    private volatile State   state               = State.STANDBY;
    private volatile long    lastTranscriptionMs = System.currentTimeMillis();
    private volatile boolean running             = true;
    private          Thread  thread;

    @org.springframework.beans.factory.annotation.Autowired
    public VoiceLoop(MicCapture micCapture,
                     SpeechToText stt,
                     CommandRouter commandRouter,
                     TextToSpeech tts,
                     AudioPlayer audioPlayer,
                     CurrentRoomRepository roomRepository,
                     VoiceProperties props) {
        this(micCapture, stt, commandRouter, tts, audioPlayer, roomRepository, props, State.STANDBY);
    }

    /** Package-private constructor for tests — allows starting in a specific state. */
    VoiceLoop(MicCapture micCapture,
              SpeechToText stt,
              CommandRouter commandRouter,
              TextToSpeech tts,
              AudioPlayer audioPlayer,
              CurrentRoomRepository roomRepository,
              VoiceProperties props,
              State initialState) {
        this.micCapture     = micCapture;
        this.stt            = stt;
        this.commandRouter  = commandRouter;
        this.tts            = tts;
        this.audioPlayer    = audioPlayer;
        this.roomRepository = roomRepository;
        this.props          = props;
        this.state          = initialState;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("VoiceLoop starting — session {} — say '{}' to activate",
                 sessionId, props.wakePhrase());
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
            if (state == State.STANDBY) {
                standbyIteration();
            } else {
                activeIteration();
                checkInactivityTimeout();
            }
        }
        log.info("VoiceLoop exited");
    }

    private void standbyIteration() {
        micCapture.capture()
            .flatMap(stt::transcribe)
            .onSuccess(transcript -> {
                if (normalize(transcript).contains(normalize(props.wakePhrase()))) {
                    log.info("[Wake] Phrase detected — activating");
                    state = State.ACTIVE;
                    lastTranscriptionMs = System.currentTimeMillis();
                    tts.synthesize("How may I assist you?")
                       .onSuccess(audioPlayer::play)
                       .onFailure(e -> log.warn("[Wake] TTS failed: {}", e.getMessage()));
                }
            })
            .onFailure(e -> {
                if (!(e instanceof SilentFrameException)) {
                    log.debug("[Standby] Iteration error: {}", e.getMessage());
                }
            });
    }

    private void activeIteration() {
        micCapture.capture()
            .onSuccess(chunk -> log.debug("[STT] Captured {} bytes, sending to Whisper", chunk.pcm().length))
            .flatMap(stt::transcribe)
            .onSuccess(transcript -> {
                lastTranscriptionMs = System.currentTimeMillis();
                log.info("[STT] Transcribed: \"{}\"", transcript);
            })
            .map(transcript -> {
                if (normalize(transcript).contains(normalize(props.sleepPhrase()))) {
                    log.info("[Sleep] Phrase detected — entering standby");
                    state = State.STANDBY;
                    return new ButlerResponse("Very well. Standing by.", sessionId);
                }
                return commandRouter.route(
                    new ButlerRequest(transcript, roomRepository.getCurrentRoom(sessionId), sessionId));
            })
            .onSuccess(response -> log.info("[Brain] Response: \"{}\"", response.response()))
            .map(ButlerResponse::response)
            .flatMap(tts::synthesize)
            .onSuccess(chunk -> log.debug("[TTS] Synthesized {} bytes, playing", chunk.pcm().length))
            .onSuccess(audioPlayer::play)
            .onFailure(e -> {
                if (e instanceof SilentFrameException) log.debug("Silent frame — skipping TTS");
                else log.warn("Voice loop iteration failed: {}", e.getMessage());
            });
    }

    private void checkInactivityTimeout() {
        if (System.currentTimeMillis() - lastTranscriptionMs > props.inactivityTimeoutMs()) {
            log.info("[Inactivity] No speech for {}ms — entering standby", props.inactivityTimeoutMs());
            state = State.STANDBY;
            lastTranscriptionMs = System.currentTimeMillis();
            tts.synthesize("I'll be standing by.")
               .onSuccess(audioPlayer::play)
               .onFailure(e -> log.warn("[Inactivity] TTS failed: {}", e.getMessage()));
        }
    }

    /** Lowercases and strips punctuation so "Hey, Jeeves!" matches "hey jeeves". */
    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }
}

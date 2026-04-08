package com.roomtour.voice.tts;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;
import com.roomtour.voice.VoiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

/**
 * Synthesizes speech by shelling out to espeak-ng.
 * --stdout produces a WAV stream; AudioSystem parses the header
 * so the returned AudioChunk carries raw PCM at the correct sample rate.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class EspeakTtsService implements TextToSpeech {

    private final VoiceProperties props;

    public EspeakTtsService(VoiceProperties props) {
        this.props = props;
    }

    @Override
    public Try<AudioChunk> synthesize(String text) {
        return Try.of(() -> {
            Process process = new ProcessBuilder(props.espeakBinary(), "--stdout", text)
                    .redirectErrorStream(true)
                    .start();

            byte[] wavBytes = process.getInputStream().readAllBytes();
            int    exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("espeak-ng exited with code " + exitCode);
            }

            try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(wavBytes))) {
                int    sampleRate = (int) ais.getFormat().getSampleRate();
                byte[] pcm        = ais.readAllBytes();
                log.debug("Synthesized {} bytes of PCM at {}Hz", pcm.length, sampleRate);
                return new AudioChunk(pcm, sampleRate);
            }
        }).onFailure(e -> log.warn("espeak-ng synthesis failed: {}", e.getMessage(), e));
    }
}

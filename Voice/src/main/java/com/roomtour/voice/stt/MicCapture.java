package com.roomtour.voice.stt;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;
import com.roomtour.voice.VoiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;

/**
 * Captures audio from the default microphone using silence-based VAD.
 * Recording starts when RMS exceeds the configured threshold and stops
 * after the configured silence window elapses (or the max duration cap).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class MicCapture {

    private static final int BITS_PER_SAMPLE  = 16;
    private static final int CHANNELS         = 1;
    private static final int FRAME_MS         = 20;
    private static final int RMS_LOG_INTERVAL = 50; // log RMS every ~1 second (50 x 20ms frames)

    private final VoiceProperties props;

    public MicCapture(VoiceProperties props) {
        this.props = props;
    }

    public Try<AudioChunk> capture() {
        return Try.of(() -> {
            AudioFormat format = new AudioFormat(props.sampleRateHz(), BITS_PER_SAMPLE, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
                line.open(format);
                line.start();
                log.debug("Mic open — waiting for speech");
                return recordWithVad(line);
            }
        }).onFailure(e -> log.error("Mic capture failed: {}", e.getMessage(), e));
    }

    private AudioChunk recordWithVad(TargetDataLine line) throws Exception {
        int frameBytes = (int) (props.sampleRateHz() * FRAME_MS / 1000.0)
                * (BITS_PER_SAMPLE / 8) * CHANNELS;
        byte[] frame  = new byte[frameBytes];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        boolean recording    = false;
        long    silenceStart = -1;
        long    captureStart = System.currentTimeMillis();
        int     frameCount   = 0;

        while (true) {
            if (System.currentTimeMillis() - captureStart >= props.maxDurationMs()) {
                log.debug("Max duration reached, stopping capture");
                break;
            }

            int bytesRead = line.read(frame, 0, frame.length);
            if (bytesRead <= 0) continue;

            double rms = computeRms(frame, bytesRead);
            if (++frameCount % RMS_LOG_INTERVAL == 0) {
                log.debug("Mic RMS={}  threshold={}  recording={}", (int) rms, props.silenceThreshold(), recording);
            }

            if (rms >= props.silenceThreshold()) {
                if (!recording) log.debug("Speech detected, recording");
                recording    = true;
                silenceStart = -1;
                buffer.write(frame, 0, bytesRead);
            } else if (recording) {
                buffer.write(frame, 0, bytesRead);
                if (silenceStart == -1) {
                    silenceStart = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - silenceStart >= props.silenceDurationMs()) {
                    log.debug("Silence window elapsed, stopping capture");
                    break;
                }
            }
        }

        return new AudioChunk(buffer.toByteArray(), props.sampleRateHz());
    }

    private double computeRms(byte[] buffer, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += (long) sample * sample;
        }
        return Math.sqrt((double) sum / (bytesRead / 2));
    }
}

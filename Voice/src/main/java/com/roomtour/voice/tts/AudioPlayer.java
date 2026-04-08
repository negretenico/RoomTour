package com.roomtour.voice.tts;

import com.roomtour.voice.AudioChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays raw PCM audio through the default system output device.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class AudioPlayer {

    public void play(AudioChunk chunk) {
        AudioFormat format = new AudioFormat(chunk.sampleRateHz(), 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();
            line.write(chunk.pcm(), 0, chunk.pcm().length);
            line.drain();
            log.debug("Playback complete: {} bytes", chunk.pcm().length);
        } catch (Exception e) {
            log.error("Audio playback failed: {}", e.getMessage(), e);
        }
    }
}

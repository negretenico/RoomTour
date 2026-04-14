package com.roomtour.voice.stt;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;
import com.roomtour.voice.VoiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Sends audio to a locally-running faster-whisper-server via its
 * OpenAI-compatible /v1/audio/transcriptions endpoint.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "butler.voice", name = "enabled", havingValue = "true")
public class WhisperSttService implements SpeechToText {

    private final WebClient        whisperWebClient;
    private final VoiceProperties  props;

    public WhisperSttService(WebClient whisperWebClient, VoiceProperties props) {
        this.whisperWebClient = whisperWebClient;
        this.props            = props;
    }

    @Override
    public Try<String> transcribe(AudioChunk chunk) {
        return Try.of(() -> {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("file", toWav(chunk))
                .filename("audio.wav")
                .contentType(MediaType.valueOf("audio/wav"));
            body.part("model", "whisper-1");
            body.part("response_format", "verbose_json");

            WhisperResponse response = whisperWebClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body.build())
                    .retrieve()
                    .bodyToMono(WhisperResponse.class)
                    .block();

            if (response == null || response.text().isBlank()) {
                throw new SilentFrameException("blank transcript");
            }
            if (response.avgLogprob() < props.logprobThreshold()) {
                log.debug("Hallucination rejected — avg_logprob={}", response.avgLogprob());
                throw new SilentFrameException("hallucination — avg_logprob=" + response.avgLogprob());
            }

            log.debug("Transcribed: {}", response.text());
            return response.text();
        }).onFailure(e -> {
            if (!(e instanceof SilentFrameException)) {
                log.warn("Whisper transcription failed: {}", e.getMessage(), e);
            }
        });
    }

    private byte[] toWav(AudioChunk chunk) {
        byte[] pcm     = chunk.pcm();
        int    rate    = chunk.sampleRateHz();
        ByteBuffer buf = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[]{'R', 'I', 'F', 'F'});
        buf.putInt(36 + pcm.length);
        buf.put(new byte[]{'W', 'A', 'V', 'E'});
        buf.put(new byte[]{'f', 'm', 't', ' '});
        buf.putInt(16);
        buf.putShort((short) 1);        // PCM
        buf.putShort((short) 1);        // mono
        buf.putInt(rate);
        buf.putInt(rate * 2);           // byteRate
        buf.putShort((short) 2);        // blockAlign
        buf.putShort((short) 16);       // bitsPerSample
        buf.put(new byte[]{'d', 'a', 't', 'a'});
        buf.putInt(pcm.length);
        buf.put(pcm);
        return buf.array();
    }

}

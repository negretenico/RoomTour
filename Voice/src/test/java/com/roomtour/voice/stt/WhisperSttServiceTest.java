package com.roomtour.voice.stt;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;
import com.roomtour.voice.VoiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhisperSttServiceTest {

    @Mock WebClient                       whisperWebClient;
    @Mock WebClient.RequestBodyUriSpec    uriSpec;
    @Mock WebClient.RequestBodySpec       bodySpec;
    @Mock WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock WebClient.ResponseSpec          responseSpec;

    private static final VoiceProperties PROPS = new VoiceProperties(
            false, "http://localhost:8000", "espeak-ng", 800, 500, 15000, 16000, -1.0,
            "hey jeeves", "that is all jeeves", 60000);

    private WhisperSttService service;

    @BeforeEach
    void setUp() {
        service = new WhisperSttService(whisperWebClient, PROPS);
    }

    @Test
    void transcriptionTextIsReturnedFromWhisperResponse() {
        stubWebClient(new WhisperResponse("hello world", List.of(new WhisperSegment(-0.3))));

        AudioChunk chunk  = new AudioChunk(new byte[]{0, 0, 0, 0}, 16000);
        Try<String> result = service.transcribe(chunk);

        assertThat(result.getOrElse(() -> null)).isEqualTo("hello world");
    }

    @Test
    void apiFailureProducesEmptyTry() {
        when(whisperWebClient.post()).thenThrow(new RuntimeException("connection refused"));

        AudioChunk chunk = new AudioChunk(new byte[]{0, 0, 0, 0}, 16000);
        assertThat(service.transcribe(chunk).getOrElse(() -> null)).isNull();
    }

    @Test
    void blankTranscriptRejectedAsSilentFrame() {
        stubWebClient(new WhisperResponse("", List.of()));

        AudioChunk chunk = new AudioChunk(new byte[]{0, 0, 0, 0}, 16000);
        Try<String> result = service.transcribe(chunk);

        assertThat(result.getOrElse(() -> null)).isNull();
    }

    @Test
    void lowLogprobRejectedAsHallucination() {
        stubWebClient(new WhisperResponse("thank you for watching", List.of(new WhisperSegment(-1.8))));

        AudioChunk chunk = new AudioChunk(new byte[]{0, 0, 0, 0}, 16000);
        Try<String> result = service.transcribe(chunk);

        assertThat(result.getOrElse(() -> null)).isNull();
    }

    @SuppressWarnings("unchecked")
    private void stubWebClient(WhisperResponse response) {
        when(whisperWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn((Mono) Mono.just(response));
    }
}

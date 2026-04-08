package com.roomtour.voice.stt;

import com.common.functionico.risky.Try;
import com.roomtour.voice.AudioChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    private WhisperSttService service;

    @BeforeEach
    void setUp() {
        service = new WhisperSttService(whisperWebClient);
    }

    @Test
    void transcriptionTextIsReturnedFromWhisperResponse() {
        stubWebClient("hello world");

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

    @SuppressWarnings("unchecked")
    private void stubWebClient(String text) {
        when(whisperWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn((Mono) Mono.just(new WhisperResponse(text)));
    }
}

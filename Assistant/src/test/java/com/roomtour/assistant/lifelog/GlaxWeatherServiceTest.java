package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.config.GlaxWeatherProperties;
import com.roomtour.assistant.core.model.WeatherSnapshot;
import com.roomtour.assistant.lifelog.model.weather.GlaxWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlaxWeatherServiceTest {

    @Mock WebClient                          glaxWeatherWebClient;
    @Mock WebClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock WebClient.RequestHeadersSpec<?>    headersSpec;
    @Mock WebClient.ResponseSpec             responseSpec;

    private GlaxWeatherService service;

    @BeforeEach
    void setUp() {
        GlaxWeatherProperties props = new GlaxWeatherProperties("Austin, TX", "http://localhost");
        service = new GlaxWeatherService(glaxWeatherWebClient, props);
    }

    @Test
    void weatherResponseIsMappedToSnapshot() {
        stubWebClient(new GlaxWeatherResponse(
                "Austin", 82.0, "clear sky", 45, 10, "F",
                "Weather in Austin is 82.0 F, 45% Humidity, clear sky"));

        WeatherSnapshot snapshot = service.fetchCurrent("Austin, TX").getOrElse(() -> null);

        assertThat(snapshot.location()).isEqualTo("Austin");
        assertThat(snapshot.condition()).isEqualTo("clear sky");
        assertThat(snapshot.temperatureF()).isEqualTo(82.0);
        assertThat(snapshot.highF()).isEqualTo(82.0);
        assertThat(snapshot.lowF()).isEqualTo(82.0);
        assertThat(snapshot.forecast()).contains("clear sky");
    }

    @Test
    void apiFailureProducesEmptyResult() {
        when(glaxWeatherWebClient.get()).thenThrow(new RuntimeException("timeout"));

        assertThat(service.fetchCurrent("Austin, TX").getOrElse(() -> null)).isNull();
    }

    @SuppressWarnings("unchecked")
    private void stubWebClient(GlaxWeatherResponse response) {
        when(glaxWeatherWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GlaxWeatherResponse.class)).thenReturn(Mono.just(response));
    }
}

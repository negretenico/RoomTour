package com.roomtour.server.recognition;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.core.model.RecognitionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationDebounceTest {

    private ClassificationDebounce debounce;

    @BeforeEach
    void setUp() {
        RecognitionProperties props = new RecognitionProperties();
        props.setWindowSize(20);
        props.setThresholdPercent(0.80);
        debounce = new ClassificationDebounce(props);
    }

    @Test
    void windowNotYetFullDoesNotConfirmRoom() {
        Maybe<String> result = Maybe.none();
        for (int i = 0; i < 15; i++) {
            result = debounce.accept(Maybe.of("kitchen"));
        }
        assertThat(result.orElse(null)).isNull();
    }

    @Test
    void thresholdMetConfirmsRoom() {
        for (int i = 0; i < 4; i++)  debounce.accept(Maybe.none());
        for (int i = 0; i < 16; i++) debounce.accept(Maybe.of("kitchen"));

        Maybe<String> result = debounce.accept(Maybe.of("kitchen"));

        assertThat(result.orElse(null)).isEqualTo("kitchen");
    }

    @Test
    void thresholdNotMetReturnsNone() {
        for (int i = 0; i < 10; i++) debounce.accept(Maybe.of("kitchen"));
        for (int i = 0; i < 10; i++) debounce.accept(Maybe.none());

        Maybe<String> result = debounce.accept(Maybe.none());

        assertThat(result.orElse(null)).isNull();
    }

    @Test
    void windowSlidesEvictingOldFrames() {
        // fill window with kitchen — threshold met
        for (int i = 0; i < 20; i++) debounce.accept(Maybe.of("kitchen"));

        // slide in 10 living-room frames — window now 10 kitchen + 10 living-room, neither at 80%
        Maybe<String> result = Maybe.none();
        for (int i = 0; i < 10; i++) {
            result = debounce.accept(Maybe.of("living-room"));
        }

        assertThat(result.orElse(null)).isNull();
    }

    @Test
    void noneFramesSuppressUpdate() {
        for (int i = 0; i < 20; i++) debounce.accept(Maybe.none());

        Maybe<String> result = debounce.accept(Maybe.none());

        assertThat(result.orElse(null)).isNull();
    }
}

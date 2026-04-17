package com.roomtour.server.recognition;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.core.model.RecognitionProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sliding-window debounce guard for room classification results.
 *
 * <p>Buffers the last {@code windowSize} frames (including inconclusive {@code Maybe.none()} results).
 * Only emits a confirmed room label when a single label occupies at least
 * {@code thresholdPercent} of the window. If no label reaches threshold, returns
 * {@link Maybe#none()} — leaving {@code GLOBAL_SESSION} unchanged is safer than writing a wrong room.
 */
@Component
public class ClassificationDebounce {

    private final int windowSize;
    private final int required;
    private final ArrayDeque<Maybe<String>> window;

    public ClassificationDebounce(RecognitionProperties props) {
        this.windowSize = props.getWindowSize();
        this.required   = (int) Math.ceil(windowSize * props.getThresholdPercent());
        this.window     = new ArrayDeque<>(windowSize);
    }

    public synchronized Maybe<String> accept(Maybe<String> classification) {
        if (window.size() == windowSize) {
            window.pollFirst();
        }
        window.addLast(classification);
        return dominantLabel();
    }

    private Maybe<String> dominantLabel() {
        return window.stream()
            .map(m -> m.orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() >= required)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Maybe::of)
            .orElse(Maybe.none());
    }
}

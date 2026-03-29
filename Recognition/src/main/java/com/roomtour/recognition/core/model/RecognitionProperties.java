package com.roomtour.recognition.core.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "butler.recognition")
public class RecognitionProperties {

    /**
     * Room label returned by the simulated classifier.
     * Leave blank to simulate an unrecognised room (returns {@code Maybe.empty()}).
     */
    private String room = "unknown";
}

package com.roomtour.recognition.config;

import com.roomtour.recognition.classify.RoomClassifier;
import com.roomtour.recognition.classify.SimulatedRoomClassifier;
import com.roomtour.recognition.core.model.RecognitionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the recognition module.
 * Registers {@link SimulatedRoomClassifier} as the default {@link RoomClassifier}.
 * Override by declaring your own {@link RoomClassifier} bean — this one will back off.
 */
@Configuration
@EnableConfigurationProperties(RecognitionProperties.class)
public class RecognitionConfig {

    private final RecognitionProperties props;

    public RecognitionConfig(RecognitionProperties props) {
        this.props = props;
    }

    @Bean
    @ConditionalOnMissingBean(RoomClassifier.class)
    public RoomClassifier roomClassifier() {
        return new SimulatedRoomClassifier(props);
    }
}

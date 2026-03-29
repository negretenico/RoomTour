package com.roomtour.recognition.config;

import com.roomtour.recognition.classify.RoomClassifier;
import com.roomtour.recognition.classify.SimulatedRoomClassifier;
import com.roomtour.recognition.classify.YoloRoomClassifier;
import com.roomtour.recognition.core.model.RecognitionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the recognition module.
 *
 * <ul>
 *   <li>{@code butler.recognition.simulation=true} (default) → {@link SimulatedRoomClassifier}</li>
 *   <li>{@code butler.recognition.simulation=false} → {@link YoloRoomClassifier}</li>
 * </ul>
 *
 * Declare your own {@link RoomClassifier} bean to override either default.
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
    @ConditionalOnProperty(prefix = "butler.recognition", name = "simulation", havingValue = "true", matchIfMissing = true)
    public RoomClassifier simulatedRoomClassifier() {
        return new SimulatedRoomClassifier(props);
    }

    @Bean
    @ConditionalOnMissingBean(RoomClassifier.class)
    @ConditionalOnProperty(prefix = "butler.recognition", name = "simulation", havingValue = "false")
    public RoomClassifier yoloRoomClassifier() {
        return new YoloRoomClassifier(props);
    }
}

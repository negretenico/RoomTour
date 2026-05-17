package com.roomtour.drone.config;

import com.roomtour.drone.DroneNavigator;
import com.roomtour.drone.EventPublishingDroneNavigator;
import com.roomtour.drone.Explorer;
import com.roomtour.drone.RosbridgeExplorer;
import com.roomtour.drone.RosbridgeNavigator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the Drone module.
 * Provides RosbridgeNavigator and RosbridgeExplorer when butler.ros2.enabled=true.
 * Simulated fallbacks live in AssistantConfig so they are available
 * even in Spring contexts that do not load auto-configuration (e.g. IT tests).
 */
@Configuration
@EnableConfigurationProperties(RosbridgeProperties.class)
public class DroneConfig {

    @Bean
    @ConditionalOnProperty(name = "butler.ros2.enabled", havingValue = "true")
    public DroneNavigator rosbridgeNavigator(RosbridgeProperties props,
                                             ApplicationEventPublisher publisher) {
        return new EventPublishingDroneNavigator(new RosbridgeNavigator(props), publisher);
    }

    @Bean
    @ConditionalOnProperty(name = "butler.ros2.enabled", havingValue = "true")
    public Explorer rosbridgeExplorer(RosbridgeProperties props) {
        return new RosbridgeExplorer(props);
    }
}

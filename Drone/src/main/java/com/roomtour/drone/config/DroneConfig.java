package com.roomtour.drone.config;

import com.roomtour.drone.DroneNavigator;
import com.roomtour.drone.RosbridgeNavigator;
import com.roomtour.drone.SimulatedDroneNavigator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the Drone module.
 * Provides RosbridgeNavigator when butler.ros2.enabled=true.
 * The simulated fallback lives in AssistantConfig so it is available
 * even in Spring contexts that do not load auto-configuration (e.g. IT tests).
 */
@Configuration
@EnableConfigurationProperties(RosbridgeProperties.class)
public class DroneConfig {

    @Bean
    @ConditionalOnProperty(name = "butler.ros2.enabled", havingValue = "true")
    public DroneNavigator rosbridgeNavigator(RosbridgeProperties props) {
        return new RosbridgeNavigator(props);
    }
}

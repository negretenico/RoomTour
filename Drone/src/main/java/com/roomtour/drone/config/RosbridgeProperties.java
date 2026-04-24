package com.roomtour.drone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "butler.ros2")
public class RosbridgeProperties {

    private boolean enabled = false;
    private String  url     = "ws://localhost:9090";
    private String  topic   = "/butler/navigate_to";
}

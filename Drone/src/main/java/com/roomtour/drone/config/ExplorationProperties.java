package com.roomtour.drone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "butler.exploration")
public class ExplorationProperties {

    /** Ordered list of rooms the simulated explorer will "discover". */
    private List<String> simulatedRooms = List.of("kitchen", "living room", "bedroom", "hallway");

    /** Milliseconds between simulated room detections. */
    private long simulatedRoomIntervalMs = 3000;
}

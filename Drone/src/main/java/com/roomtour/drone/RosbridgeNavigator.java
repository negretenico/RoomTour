package com.roomtour.drone;

import com.roomtour.drone.config.RosbridgeProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

/**
 * Sends a navigation goal to the robot via rosbridge WebSocket.
 *
 * Publishes a std_msgs/String to the configured topic (default: /butler/navigate_to).
 * The robot-side ROS2 node translates the room name to a pose and forwards it to Nav2.
 *
 * A new WebSocket connection is opened per navigate() call (connect → send → close).
 * This keeps Java stateless and avoids managing reconnect logic at this phase.
 */
@Slf4j
public class RosbridgeNavigator implements DroneNavigator {

    private final RosbridgeProperties props;

    public RosbridgeNavigator(RosbridgeProperties props) {
        this.props = props;
    }

    @Override
    public void navigate(String destination) {
        String payload = buildPayload(destination);
        log.info("[ROS2] Sending nav goal to '{}' via {}", destination, props.getUrl());
        try {
            HttpClient client = HttpClient.newHttpClient();
            WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(props.getUrl()), new WebSocket.Listener() {})
                .join();
            ws.sendText(payload, true).join();
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        } catch (Exception e) {
            log.error("[ROS2] Failed to send nav goal to '{}': {}", destination, e.getMessage());
        }
    }

    String buildPayload(String destination) {
        return "{\"op\":\"publish\",\"topic\":\"" + props.getTopic() + "\"," +
               "\"msg\":{\"data\":\"" + destination + "\"}}";
    }
}

package com.roomtour.drone;

import com.roomtour.drone.config.RosbridgeProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

/**
 * Sends start/stop exploration signals to the robot via rosbridge WebSocket.
 *
 * Publishes a std_msgs/Bool to {@code butler.ros2.explore-topic} (default: /butler/explore).
 * The robot-side ROS2 node forwards the signal to explore_lite.
 */
@Slf4j
public class RosbridgeExplorer implements Explorer {

    private final RosbridgeProperties props;

    public RosbridgeExplorer(RosbridgeProperties props) {
        this.props = props;
    }

    @Override
    public void start() {
        publish(true);
    }

    @Override
    public void stop() {
        publish(false);
    }

    private void publish(boolean active) {
        String payload = "{\"op\":\"publish\",\"topic\":\"" + props.getExploreTopic() + "\"," +
                         "\"msg\":{\"data\":" + active + "}}";
        log.info("[ROS2] Publishing explore={} to {}", active, props.getExploreTopic());
        try {
            HttpClient client = HttpClient.newHttpClient();
            WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(props.getUrl()), new WebSocket.Listener() {})
                .join();
            ws.sendText(payload, true).join();
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        } catch (Exception e) {
            log.error("[ROS2] Failed to publish explore={}: {}", active, e.getMessage());
        }
    }
}

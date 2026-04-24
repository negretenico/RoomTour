package com.roomtour.drone;

import com.roomtour.drone.config.RosbridgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RosbridgeNavigatorTest {

    RosbridgeProperties props;
    RosbridgeNavigator  navigator;

    @BeforeEach
    void setUp() {
        props = new RosbridgeProperties();
        props.setUrl("ws://localhost:9090");
        props.setTopic("/butler/navigate_to");
        navigator = new RosbridgeNavigator(props);
    }

    @Test
    void buildPayload_publishesDestinationAsStdMsgsString() {
        String payload = navigator.buildPayload("bedroom");
        assertThat(payload).isEqualTo(
            "{\"op\":\"publish\",\"topic\":\"/butler/navigate_to\",\"msg\":{\"data\":\"bedroom\"}}"
        );
    }

    @Test
    void buildPayload_usesConfiguredTopic() {
        props.setTopic("/custom/navigate");
        String payload = navigator.buildPayload("kitchen");
        assertThat(payload).contains("\"topic\":\"/custom/navigate\"");
    }

    @Test
    void buildPayload_embedsDestinationInMsg() {
        String payload = navigator.buildPayload("living room");
        assertThat(payload).contains("\"data\":\"living room\"");
    }
}

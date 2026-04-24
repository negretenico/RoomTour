package com.roomtour.drone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class SimulatedDroneNavigatorTest {

    private final SimulatedDroneNavigator navigator = new SimulatedDroneNavigator();

    @Test
    void navigate_doesNotThrow() {
        assertThatCode(() -> navigator.navigate("bedroom"))
            .doesNotThrowAnyException();
    }
}

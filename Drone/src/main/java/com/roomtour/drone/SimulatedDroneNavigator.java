package com.roomtour.drone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimulatedDroneNavigator implements DroneNavigator {

    @Override
    public void navigate(String destination) {
        log.info("[SIM] Would navigate to '{}'", destination);
    }
}

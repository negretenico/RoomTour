package com.roomtour.drone;

/**
 * Sends a navigation goal to the robot.
 * The robot owns room-to-pose translation and path execution via Nav2.
 */
public interface DroneNavigator {
    void navigate(String destination);
}

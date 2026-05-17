package com.roomtour.drone;

/**
 * Starts and stops autonomous frontier-based exploration on the robot.
 * The robot owns movement; this interface only signals intent.
 */
public interface Explorer {
    void start();
    void stop();
}

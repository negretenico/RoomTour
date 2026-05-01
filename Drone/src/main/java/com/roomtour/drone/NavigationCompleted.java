package com.roomtour.drone;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a navigation command is sent to the robot.
 * Note: "completed" means the command was dispatched, not that the drone has physically arrived.
 */
public class NavigationCompleted extends ApplicationEvent {

    private final String destination;

    public NavigationCompleted(Object source, String destination) {
        super(source);
        this.destination = destination;
    }

    public String destination() {
        return destination;
    }
}

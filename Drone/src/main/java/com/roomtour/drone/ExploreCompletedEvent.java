package com.roomtour.drone;

import org.springframework.context.ApplicationEvent;

/**
 * Published when autonomous exploration finishes — either because the robot exhausted
 * all frontiers ({@link SimulatedExplorer} or ROS2 webhook) or the user stopped it manually.
 */
public class ExploreCompletedEvent extends ApplicationEvent {

    public ExploreCompletedEvent(Object source) {
        super(source);
    }
}

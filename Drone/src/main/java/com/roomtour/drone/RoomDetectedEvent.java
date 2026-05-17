package com.roomtour.drone;

import org.springframework.context.ApplicationEvent;

/**
 * Published after the sliding-window debounce confirms a room classification.
 * Fired by the recognition pipeline (real hardware) or {@link SimulatedExplorer} (simulation).
 */
public class RoomDetectedEvent extends ApplicationEvent {

    private final String room;

    public RoomDetectedEvent(Object source, String room) {
        super(source);
        this.room = room;
    }

    public String room() {
        return room;
    }
}

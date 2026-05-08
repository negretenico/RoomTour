package com.roomtour.server.recognition;

import org.springframework.context.ApplicationEvent;

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

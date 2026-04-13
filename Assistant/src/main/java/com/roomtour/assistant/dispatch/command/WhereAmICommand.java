package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import org.springframework.stereotype.Component;

@Component
public class WhereAmICommand implements ButlerCommand {

    private final CurrentRoomRepository roomRepository;

    public WhereAmICommand(CurrentRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override public String token() { return "/where-am-i"; }
    @Override public String usage() { return "/where-am-i"; }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String room = roomRepository.getCurrentRoom(sessionId);
        String location = (room == null || room.isBlank() || room.equals("unknown"))
            ? "an unrecognised room" : room;
        return new ButlerResponse("You are in the " + location + ".", sessionId);
    }
}

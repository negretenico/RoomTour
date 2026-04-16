package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WhereAmICommand implements ButlerCommand {

    private static final Pattern INTENT = Pattern.compile(
        "(?i)(where\\s+am\\s+i|what\\s+room|what\\s+is\\s+this\\s+place|where\\s+are\\s+we|what\\s+room\\s+am\\s+i)"
    );

    private final CurrentRoomRepository roomRepository;

    public WhereAmICommand(CurrentRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override public String token() { return "/where-am-i"; }
    @Override public String usage() { return "/where-am-i"; }
    @Override public Optional<Pattern> intentPattern() { return Optional.of(INTENT); }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String room = roomRepository.getCurrentRoom(sessionId);
        String location = (room == null || room.isBlank() || room.equals("unknown"))
            ? "an unrecognised room" : room;
        log.info("[WHERE-AM-I] session={} room={}", sessionId, location);
        return new ButlerResponse("You are in the " + location + ".", sessionId);
    }
}

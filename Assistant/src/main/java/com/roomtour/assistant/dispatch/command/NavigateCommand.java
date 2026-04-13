package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import com.roomtour.assistant.navigation.PathfindingService;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class NavigateCommand implements ButlerCommand {

    private final PathfindingService pathfinder;
    private final RoomGraphHolder graphHolder;
    private final CurrentRoomRepository roomRepository;

    public NavigateCommand(PathfindingService pathfinder,
                           RoomGraphHolder graphHolder,
                           CurrentRoomRepository roomRepository) {
        this.pathfinder     = pathfinder;
        this.graphHolder    = graphHolder;
        this.roomRepository = roomRepository;
    }

    @Override public String token() { return "/navigate"; }
    @Override public String usage() { return "/navigate <room>"; }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String target = message.substring(token().length()).strip();
        if (target.isBlank()) {
            return new ButlerResponse("Usage: /navigate <room name>", sessionId);
        }
        if (graphHolder.get().isEmpty()) {
            return new ButlerResponse("No map yet. Use /map to start building your home map.", sessionId);
        }
        String currentRoom = roomRepository.getCurrentRoom(sessionId);
        if (currentRoom == null || currentRoom.isBlank() || currentRoom.equals("unknown")) {
            return new ButlerResponse("I don't know where you are right now.", sessionId);
        }
        AtomicReference<String> errorMsg = new AtomicReference<>("Could not find a path to " + target + ".");
        return pathfinder.findPath(currentRoom, target)
            .map(path -> new ButlerResponse("Go: " + String.join(" \u2192 ", path), sessionId))
            .onFailure(e -> errorMsg.set(e.getMessage()))
            .getOrElse(() -> new ButlerResponse(errorMsg.get(), sessionId));
    }
}

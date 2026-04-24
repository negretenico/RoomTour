package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import com.roomtour.assistant.navigation.PathfindingService;
import com.roomtour.drone.DroneNavigator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class NavigateCommand implements ButlerCommand {

    private static final Pattern INTENT = Pattern.compile(
        "(?i)\\b(navigate(?:\\s+to)?|go\\s+to|take\\s+me\\s+to)\\b\\s*(.*)"
    );

    private final PathfindingService    pathfinder;
    private final RoomGraphHolder       graphHolder;
    private final CurrentRoomRepository roomRepository;
    private final DroneNavigator        droneNavigator;

    public NavigateCommand(PathfindingService pathfinder,
                           RoomGraphHolder graphHolder,
                           CurrentRoomRepository roomRepository,
                           DroneNavigator droneNavigator) {
        this.pathfinder     = pathfinder;
        this.graphHolder    = graphHolder;
        this.roomRepository = roomRepository;
        this.droneNavigator = droneNavigator;
    }

    @Override public String token() { return "/navigate"; }
    @Override public String usage() { return "/navigate <room>"; }
    @Override public Optional<Pattern> intentPattern() { return Optional.of(INTENT); }

    @Override
    public ButlerResponse intentExecute(String rawMessage, String sessionId) {
        Matcher m = INTENT.matcher(rawMessage.strip());
        if (!m.find()) return execute(token(), sessionId);
        String destination = m.group(2).strip();
        log.info("[NAVIGATE] intent matched destination='{}'", destination);
        return execute(destination.isBlank() ? token() : token() + " " + destination, sessionId);
    }

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
            .map(path -> {
                droneNavigator.navigate(path.getLast());
                return new ButlerResponse("Go: " + String.join(" \u2192 ", path), sessionId);
            })
            .onFailure(e -> errorMsg.set(e.getMessage()))
            .getOrElse(() -> new ButlerResponse(errorMsg.get(), sessionId));
    }
}

package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.config.NavigationProperties;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.TranscriptCleaner;
import com.roomtour.assistant.navigation.BuildMode;

import java.util.Optional;
import java.util.regex.Pattern;
import com.roomtour.assistant.navigation.ConnectionPatternParser;
import com.roomtour.assistant.navigation.GraphBuildingService;
import com.roomtour.assistant.navigation.GraphBuildingServiceFactory;
import com.roomtour.assistant.navigation.GraphPersistenceService;
import com.roomtour.assistant.navigation.MapBuildingSession;
import com.roomtour.assistant.navigation.RoomGraph;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MapCommand implements ButlerCommand {

    private static final Pattern INTENT = Pattern.compile(
        "(?i)\\b(map|start\\s+map|build\\s+map|create\\s+map|map\\s+my|map\\s+the)\\b"
    );

    private final MapBuildingSession mapSession;
    private final NavigationProperties navProps;
    private final GraphBuildingServiceFactory graphFactory;
    private final ConnectionPatternParser patternParser;
    private final GraphPersistenceService graphPersistence;
    private final RoomGraphHolder graphHolder;

    public MapCommand(MapBuildingSession mapSession,
                      NavigationProperties navProps,
                      GraphBuildingServiceFactory graphFactory,
                      ConnectionPatternParser patternParser,
                      GraphPersistenceService graphPersistence,
                      RoomGraphHolder graphHolder) {
        this.mapSession      = mapSession;
        this.navProps        = navProps;
        this.graphFactory    = graphFactory;
        this.patternParser   = patternParser;
        this.graphPersistence = graphPersistence;
        this.graphHolder     = graphHolder;
    }

    @Override public String token() { return "/map"; }
    @Override public String usage() { return "/map [description]"; }
    @Override public Optional<Pattern> intentPattern() { return Optional.of(INTENT); }

    @Override
    public ButlerResponse intentExecute(String rawMessage, String sessionId) {
        java.util.regex.Matcher m = INTENT.matcher(rawMessage);
        if (!m.find()) return execute(token(), sessionId);

        String trigger = m.group(1).strip();
        String rest    = TranscriptCleaner.stripPunctuation(rawMessage.substring(m.end()));

        if (!rest.isBlank()) return execute(token() + " " + rest, sessionId);

        // Any map trigger ("map", "start map", "build map" …) always opens a session.
        // Seed with the existing persisted graph so prior rooms are not lost.
        // The slash command /map retains the original show-summary behaviour.
        RoomGraph existing = graphHolder.get();
        GraphBuildingService service = existing.isEmpty()
            ? graphFactory.create(BuildMode.VOICE)
            : graphFactory.createFrom(existing);
        log.info("[MAP] session={} starting map session via '{}' — seeding with {} existing room(s)",
            sessionId, trigger, existing.getRooms().size());
        mapSession.start(sessionId, service);
        String prompt = existing.isEmpty()
            ? navProps.getMapPrompt()
            : "Loaded " + existing.getRooms().size() + " room(s): " + String.join(", ", existing.getRooms().values())
              + ". Keep describing connections, or say 'done' when finished.";
        return new ButlerResponse(prompt, sessionId);
    }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String args = message.substring(token().length()).strip();

        if (args.isEmpty()) {
            if (mapSession.isActive(sessionId)) {
                return mapSession.getService(sessionId)
                    .map(svc -> new ButlerResponse(svc.getGraph().summary(), sessionId))
                    .orElse(new ButlerResponse("No active mapping session.", sessionId));
            }
            RoomGraph current = graphHolder.get();
            return current.isEmpty() ? startSession(sessionId) : new ButlerResponse(current.summary(), sessionId);
        }

        return parseInto(args, sessionId);
    }

    /** Called by the router when a session is active and free-text arrives. */
    public ButlerResponse handleFreeText(String message, String sessionId) {
        // Strip a leading map trigger if the user says "map kitchen connects to X"
        // while already in a map session — prevents "map kitchen" becoming the room name.
        String normalized = stripMapPrefix(message);

        if (isDoneKeyword(normalized)) {
            return mapSession.getService(sessionId)
                .map(svc -> {
                    RoomGraph built = svc.getGraph();
                    graphPersistence.save(built)
                        .onSuccess(path -> graphHolder.set(built))
                        .onFailure(e -> log.error("Failed to save room graph: {}", e.getMessage(), e));
                    mapSession.end(sessionId);
                    log.info("[MAP] session={} map-building complete: {}", sessionId, built.summary());
                    return new ButlerResponse("Map saved! " + built.summary(), sessionId);
                })
                .orElse(new ButlerResponse("No active mapping session.", sessionId));
        }
        return parseInto(normalized, sessionId);
    }

    /** Strips a leading map trigger word (e.g. "map") from a message if present. */
    private String stripMapPrefix(String message) {
        java.util.regex.Matcher m = INTENT.matcher(message);
        if (!m.find() || m.start() != 0) return message;
        String rest = TranscriptCleaner.stripPunctuation(message.substring(m.end()));
        return rest.isBlank() ? message : rest;
    }

    /**
     * Checks whether the message is the map-done keyword.
     * Uses a word-boundary regex so "Done." and "I'm done" both match,
     * compensating for Whisper punctuation and sentence-level transcripts.
     */
    private boolean isDoneKeyword(String message) {
        String keyword = java.util.regex.Pattern.quote(navProps.getMapDoneKeyword());
        return java.util.regex.Pattern.compile("(?i)\\b" + keyword + "\\b")
            .matcher(message).find();
    }

    public boolean isSessionActive(String sessionId) {
        return mapSession.isActive(sessionId);
    }

    private ButlerResponse parseInto(String description, String sessionId) {
        if (!mapSession.isActive(sessionId)) {
            mapSession.start(sessionId, graphFactory.create(BuildMode.VOICE));
        }
        return mapSession.getService(sessionId)
            .map(svc -> patternParser.parse(description, svc)
                .map(msg -> new ButlerResponse(msg, sessionId))
                .getOrElse(() -> new ButlerResponse(
                    "Could not parse that. Try: 'kitchen connects to the living room'.", sessionId)))
            .orElse(new ButlerResponse("Could not start mapping session.", sessionId));
    }

    private ButlerResponse startSession(String sessionId) {
        mapSession.start(sessionId, graphFactory.create(BuildMode.VOICE));
        return new ButlerResponse(navProps.getMapPrompt(), sessionId);
    }
}

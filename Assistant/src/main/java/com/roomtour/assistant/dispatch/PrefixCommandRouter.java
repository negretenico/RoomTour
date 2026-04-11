package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import com.common.functionico.value.Maybe;
import com.roomtour.assistant.config.NavigationProperties;
import com.roomtour.assistant.navigation.BuildMode;
import com.roomtour.assistant.navigation.ConnectionPatternParser;
import com.roomtour.assistant.navigation.GraphBuildingServiceFactory;
import com.roomtour.assistant.navigation.GraphPersistenceService;
import com.roomtour.assistant.navigation.MapBuildingSession;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Routes requests by prefix: messages starting with '/' are dispatched as commands
 * immediately (single-shot, no history). All other messages delegate to the
 * conversation service — unless the session is in MAP_BUILDING mode, in which
 * case free-text is interpreted as room connection descriptions.
 */
@Slf4j
public class PrefixCommandRouter implements CommandRouter {

    private final ChatService<ButlerResponse, ButlerRequest> chatService;
    private final LifelogService lifelogService;
    private final ClaudeClient claudeClient;
    private final ButlerProperties butlerProps;
    private final NavigationProperties navProps;
    private final MapBuildingSession mapSession;
    private final GraphPersistenceService graphPersistence;
    private final GraphBuildingServiceFactory graphFactory;
    private final ConnectionPatternParser patternParser;

    public PrefixCommandRouter(ChatService<ButlerResponse, ButlerRequest> chatService,
                                LifelogService lifelogService,
                                ClaudeClient claudeClient,
                                ButlerProperties butlerProps,
                                NavigationProperties navProps,
                                MapBuildingSession mapSession,
                                GraphPersistenceService graphPersistence,
                                GraphBuildingServiceFactory graphFactory,
                                ConnectionPatternParser patternParser) {
        this.chatService      = chatService;
        this.lifelogService   = lifelogService;
        this.claudeClient     = claudeClient;
        this.butlerProps      = butlerProps;
        this.navProps         = navProps;
        this.mapSession       = mapSession;
        this.graphPersistence = graphPersistence;
        this.graphFactory     = graphFactory;
        this.patternParser    = patternParser;
    }

    @Override
    public ButlerResponse route(ButlerRequest request) {
        String message   = request.message().strip();
        String sessionId = resolveSessionId(request.sessionId());

        if (mapSession.isActive(sessionId) && !message.startsWith("/")) {
            return handleMapInput(message, sessionId);
        }

        if (!message.startsWith("/")) {
            return chatService.chat(request);
        }

        return switch (commandToken(message)) {
            case "/whats-new"   -> whatsNew(sessionId);
            case "/brief"       -> brief(sessionId);
            case "/add-note"    -> addNote(message, sessionId);
            case "/where-am-i"  -> whereAmI(request.room(), sessionId);
            case "/map"         -> map(message, sessionId);
            case "/commands"    -> commands(sessionId);
            default             -> unknown(commandToken(message), sessionId);
        };
    }

    private String resolveSessionId(String sessionId) {
        return Maybe.of(sessionId)
            .map(s -> s.isBlank() ? null : s)
            .orElse(UUID.randomUUID().toString());
    }

    private String commandToken(String message) {
        int space = message.indexOf(' ');
        return space == -1 ? message : message.substring(0, space);
    }

    // --- /map -----------------------------------------------------------

    private ButlerResponse map(String message, String sessionId) {
        String args = message.substring("/map".length()).strip();

        if (args.isEmpty()) {
            if (mapSession.isActive(sessionId)) {
                return mapSession.getService(sessionId)
                    .map(svc -> new ButlerResponse(svc.getGraph().summary(), sessionId))
                    .orElse(new ButlerResponse("No active mapping session.", sessionId));
            }
            return graphPersistence.load()
                .map(graph -> graph.isEmpty()
                    ? startSession(sessionId)
                    : new ButlerResponse(graph.summary(), sessionId))
                .getOrElse(() -> startSession(sessionId));
        }

        return parseInto(args, sessionId);
    }

    private ButlerResponse handleMapInput(String message, String sessionId) {
        if (message.equalsIgnoreCase(navProps.getMapDoneKeyword())) {
            return mapSession.getService(sessionId)
                .map(svc -> {
                    graphPersistence.save(svc.getGraph())
                        .onFailure(e -> log.error("Failed to save room graph: {}", e.getMessage(), e));
                    mapSession.end(sessionId);
                    return new ButlerResponse("Map saved! " + svc.getGraph().summary(), sessionId);
                })
                .orElse(new ButlerResponse("No active mapping session.", sessionId));
        }
        return parseInto(message, sessionId);
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

    // --- existing commands ----------------------------------------------

    private ButlerResponse whatsNew(String sessionId) {
        String context = lifelogService.formatForPrompt();
        String response = context.isBlank()
            ? "Nothing new in your lifelog."
            : "Here's what's going on:\n" + context;
        return new ButlerResponse(response, sessionId);
    }

    private ButlerResponse brief(String sessionId) {
        String prompt = String.format(
            "You are %s, %s's home drone butler. Give a concise daily brief based on this context:%n%s",
            butlerProps.getName(), butlerProps.getUserName(), lifelogService.formatForPrompt()
        );
        return claudeClient.send(prompt, List.of())
            .map(text -> new ButlerResponse(text, sessionId))
            .getOrElse(() -> new ButlerResponse("Unable to generate brief right now.", sessionId));
    }

    private ButlerResponse addNote(String message, String sessionId) {
        String note = message.substring("/add-note".length()).strip();
        if (note.isBlank()) {
            return unknown("/add-note", sessionId);
        }
        lifelogService.addNote(note);
        return new ButlerResponse("Got it, I've noted: \"" + note + "\"", sessionId);
    }

    private ButlerResponse commands(String sessionId) {
        String list = "/commands | /where-am-i | /whats-new | /brief | /add-note <text> | /map [description]";
        return new ButlerResponse(list, sessionId);
    }

    private ButlerResponse whereAmI(String room, String sessionId) {
        String location = (room == null || room.isBlank() || room.equals("unknown"))
            ? "an unrecognised room" : room;
        return new ButlerResponse("You are in the " + location + ".", sessionId);
    }

    private ButlerResponse unknown(String token, String sessionId) {
        return new ButlerResponse("Unknown command: " + token, sessionId);
    }
}

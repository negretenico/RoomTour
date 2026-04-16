package com.roomtour.assistant.dispatch;

import com.common.functionico.risky.Try;
import com.common.functionico.value.Maybe;
import com.roomtour.assistant.ai.RegistryStubClaudeClient;
import com.roomtour.assistant.chat.ButlerChatService;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.config.NavigationProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.dispatch.command.MapCommand;
import com.roomtour.assistant.dispatch.command.NavigateCommand;
import com.roomtour.assistant.dispatch.command.WhatsNewCommand;
import com.roomtour.assistant.dispatch.command.WhereAmICommand;
import com.roomtour.assistant.lifelog.LifelogService;
import com.roomtour.assistant.navigation.ConnectionPatternParser;
import com.roomtour.assistant.navigation.GraphBuildingServiceFactory;
import com.roomtour.assistant.navigation.GraphPersistenceService;
import com.roomtour.assistant.navigation.MapBuildingSession;
import com.roomtour.assistant.navigation.PathfindingService;
import com.roomtour.assistant.navigation.RoomGraph;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import com.roomtour.assistant.navigation.VoiceGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies end-to-end voice routing: a spoken phrase (simulated by passing a
 * fixed transcript directly to the router) reaches the correct ButlerCommand or
 * ChatService. STT is not involved — we test the routing layer in isolation.
 *
 * Covers both prefix routing (/token) and intent-pattern routing (natural language).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoiceRoutingTest {

    static final String SESSION     = "voice-session";
    static final String MAP_SESSION = "map-session";

    @Mock CurrentRoomRepository roomRepository;
    @Mock LifelogService        lifelogService;
    @Mock MapBuildingSession    mapSession;
    @Mock GraphBuildingServiceFactory graphFactory;
    @Mock GraphPersistenceService     graphPersistence;
    @Mock RoomGraphHolder             graphHolder;
    @Mock PathfindingService          pathfinder;

    PrefixCommandRouter      router;
    RegistryStubClaudeClient stubClient;
    NavigationProperties     navProps = new NavigationProperties();

    @BeforeEach
    void setUp() {
        stubClient = new RegistryStubClaudeClient("Good morning, sir. How may I be of assistance?")
            .register("Hello", "Good day! How can I help you?");

        when(lifelogService.formatForPrompt()).thenReturn("");

        ButlerChatService chatService = new ButlerChatService(lifelogService, stubClient, new ButlerProperties());
        ConnectionPatternParser parser = new ConnectionPatternParser(navProps);

        WhereAmICommand whereAmI = new WhereAmICommand(roomRepository);
        WhatsNewCommand whatsNew = new WhatsNewCommand(lifelogService);
        MapCommand      map      = new MapCommand(mapSession, navProps, graphFactory, parser, graphPersistence, graphHolder);
        NavigateCommand navigate = new NavigateCommand(pathfinder, graphHolder, roomRepository);

        router = new PrefixCommandRouter(chatService, List.of(whereAmI, whatsNew, map, navigate), map);
    }

    // ── ChatService fallback ──────────────────────────────────────────────────

    @Test
    void helloRoutesToChatServiceAndReturnsRegisteredStub() {
        ButlerResponse response = router.route(new ButlerRequest("Hello", "hallway", SESSION));
        assertThat(response.response()).isEqualTo("Good day! How can I help you?");
    }

    @Test
    void utteranceWithNoMatchUsesDefaultStub() {
        ButlerResponse response = router.route(new ButlerRequest("Tell me a story", "hallway", SESSION));
        assertThat(response.response()).isEqualTo("Good morning, sir. How may I be of assistance?");
    }

    // ── Intent routing ────────────────────────────────────────────────────────

    @Test
    void whereAmIIntentRoutesToWhereAmICommand() {
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("living room");
        ButlerResponse response = router.route(new ButlerRequest("where am I", null, SESSION));
        assertThat(response.response()).isEqualTo("You are in the living room.");
    }

    @Test
    void whatIsThisPlaceIntentRoutesToWhereAmICommand() {
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("kitchen");
        ButlerResponse response = router.route(new ButlerRequest("what is this place", null, SESSION));
        assertThat(response.response()).isEqualTo("You are in the kitchen.");
    }

    @Test
    void whatsNewIntentRoutesToWhatsNewCommand() {
        when(lifelogService.formatForPrompt()).thenReturn("Meeting at 9am.");
        ButlerResponse response = router.route(new ButlerRequest("whats new", null, SESSION));
        assertThat(response.response()).contains("Meeting at 9am.");
    }

    @Test
    void startMapIntentStartsMapSession() {
        when(mapSession.isActive(SESSION)).thenReturn(false);
        when(graphHolder.get()).thenReturn(new RoomGraph());

        ButlerResponse response = router.route(new ButlerRequest("start map", null, SESSION));
        assertThat(response.response()).contains("Start describing your home");
    }

    @Test
    void mapWithInlineDescriptionParsesConnectionDirectly() {
        VoiceGraphBuilder builder = new VoiceGraphBuilder(new RoomGraph());
        when(mapSession.isActive(SESSION)).thenReturn(false);
        when(mapSession.getService(SESSION)).thenReturn(Maybe.of(builder));

        router.route(new ButlerRequest("map room a connects to room b", null, SESSION));

        assertThat(builder.getGraph().getRooms()).containsKey("room a");
        assertThat(builder.getGraph().getRooms()).containsKey("room b");
    }

    // ── Prefix routing ────────────────────────────────────────────────────────

    @Test
    void slashWhereAmIUsesPrefix() {
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("bedroom");
        ButlerResponse response = router.route(new ButlerRequest("/where-am-i", null, SESSION));
        assertThat(response.response()).isEqualTo("You are in the bedroom.");
    }

    @Test
    void slashNavigateRoutesByPrefix() {
        when(graphHolder.get()).thenReturn(new RoomGraph()); // empty → "No map yet" response from NavigateCommand
        ButlerResponse response = router.route(new ButlerRequest("/navigate bedroom", null, SESSION));
        assertThat(response.response()).containsIgnoringCase("map");
    }

    @Test
    void unknownSlashCommandReturnsErrorMessage() {
        ButlerResponse response = router.route(new ButlerRequest("/unknown-cmd", null, SESSION));
        assertThat(response.response()).containsIgnoringCase("Unknown command").contains("/unknown-cmd");
    }

    // ── Map-building session flow ─────────────────────────────────────────────

    @Test
    void activeMapSessionInterceptsFreeTextAndAddsRoom() {
        VoiceGraphBuilder builder = new VoiceGraphBuilder(new RoomGraph());
        when(mapSession.isActive(MAP_SESSION)).thenReturn(true);
        when(mapSession.getService(MAP_SESSION)).thenReturn(Maybe.of(builder));

        router.route(new ButlerRequest("kitchen connects to living room", null, MAP_SESSION));

        assertThat(builder.getGraph().getRooms()).containsKey("kitchen");
        assertThat(builder.getGraph().getRooms()).containsKey("living room");
    }

    @Test
    void doneKeywordFinishesMapSessionAndSavesGraph() {
        RoomGraph graph = new RoomGraph();
        graph.addConnection("kitchen", "living room", 1.0);
        VoiceGraphBuilder builder = new VoiceGraphBuilder(graph);

        when(mapSession.isActive(MAP_SESSION)).thenReturn(true);
        when(mapSession.getService(MAP_SESSION)).thenReturn(Maybe.of(builder));
        when(graphPersistence.save(any())).thenReturn(Try.of(() -> "saved"));

        ButlerResponse response = router.route(new ButlerRequest("done", null, MAP_SESSION));
        assertThat(response.response()).contains("Map saved!");
        verify(mapSession).end(MAP_SESSION);
    }
}

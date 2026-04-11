package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.config.NavigationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionPatternParserTest {

    private ConnectionPatternParser parser;
    private GraphBuildingService service;

    @BeforeEach
    void setUp() {
        parser  = new ConnectionPatternParser(new NavigationProperties());
        service = new VoiceGraphBuilder(new RoomGraph());
    }

    @Test
    void connectsToPatternAddsEdgeAndReturnsConfirmation() {
        Try<String> result = parser.parse("kitchen connects to the living room", service);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrElse(() -> "")).contains("kitchen");
        assertThat(result.getOrElse(() -> "")).contains("living room");
    }

    @Test
    void leadsToPatternIsRecognised() {
        Try<String> result = parser.parse("hallway leads to the bedroom", service);

        assertThat(result.isSuccess()).isTrue();
        assertThat(service.getGraph().getRooms()).containsKey("hallway");
        assertThat(service.getGraph().getRooms()).containsKey("bedroom");
    }

    @Test
    void isNextToPatternIsRecognised() {
        Try<String> result = parser.parse("office is next to the library", service);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void isAdjacentToPatternIsRecognised() {
        Try<String> result = parser.parse("garage is adjacent to the kitchen", service);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void leadingArticlesOnBothRoomsAreStripped() {
        parser.parse("the kitchen connects to the living room", service);

        assertThat(service.getGraph().getRooms()).containsKey("kitchen");
        assertThat(service.getGraph().getRooms()).containsKey("living room");
    }

    @Test
    void caseIsNormalisedDuringParsing() {
        parser.parse("Kitch connects to Living Room", service);

        assertThat(service.getGraph().getRooms()).containsKey("kitch");
        assertThat(service.getGraph().getRooms()).containsKey("living room");
    }

    @Test
    void edgesAreBidirectional() {
        parser.parse("kitchen connects to living room", service);

        assertThat(service.getGraph().getAdjacency().get("kitchen")).containsKey("living room");
        assertThat(service.getGraph().getAdjacency().get("living room")).containsKey("kitchen");
    }

    @Test
    void unrecognisedSentenceReturnsFailure() {
        Try<String> result = parser.parse("this makes no sense at all", service);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void emptyVerbSidesAreRejected() {
        Try<String> result = parser.parse("connects to", service);

        assertThat(result.isSuccess()).isFalse();
    }
}

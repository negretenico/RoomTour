package com.roomtour.assistant.navigation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomGraphTest {

    private RoomGraph graph;

    @BeforeEach
    void setUp() {
        graph = new RoomGraph();
    }

    @Test
    void addingAConnectionUpsertsBothRoomsAndABidirectionalEdge() {
        graph.addConnection("kitchen", "living room", 1.0);

        assertThat(graph.getRooms()).containsKey("kitchen");
        assertThat(graph.getRooms()).containsKey("living room");
        assertThat(graph.getAdjacency().get("kitchen")).containsKey("living room");
        assertThat(graph.getAdjacency().get("living room")).containsKey("kitchen");
    }

    @Test
    void duplicateRoomNamesAreDeduplicatedByNormalizedKey() {
        graph.addRoom("Kitchen");
        graph.addRoom("kitchen");
        graph.addRoom("the Kitchen");

        assertThat(graph.getRooms()).hasSize(1);
        assertThat(graph.getRooms()).containsKey("kitchen");
    }

    @Test
    void leadingArticlesAreStrippedDuringNormalization() {
        graph.addConnection("the kitchen", "a living room", 1.0);

        assertThat(graph.getRooms()).containsKey("kitchen");
        assertThat(graph.getRooms()).containsKey("living room");
    }

    @Test
    void firstMentionDisplayNameIsPreserved() {
        graph.addRoom("Kitchen");
        graph.addRoom("kitchen");

        assertThat(graph.getRooms().get("kitchen")).isEqualTo("Kitchen");
    }

    @Test
    void duplicateConnectionDoesNotCreateAdditionalEdges() {
        graph.addConnection("kitchen", "living room", 1.0);
        graph.addConnection("kitchen", "living room", 1.0);

        assertThat(graph.getAdjacency().get("kitchen")).hasSize(1);
    }

    @Test
    void isEmptyReturnsTrueForAFreshGraph() {
        assertThat(graph.isEmpty()).isTrue();
    }

    @Test
    void isEmptyReturnsFalseAfterAddingARoom() {
        graph.addRoom("bedroom");
        assertThat(graph.isEmpty()).isFalse();
    }

    @Test
    void summaryListsAllRoomsAndEdges() {
        graph.addConnection("kitchen", "living room", 1.0);
        String summary = graph.summary();

        assertThat(summary).contains("kitchen");
        assertThat(summary).contains("living room");
        assertThat(summary).contains("↔");
    }

    @Test
    void summaryDeduplicatesBidirectionalEdges() {
        graph.addConnection("kitchen", "living room", 1.0);
        long arrowCount = graph.summary().chars()
            .filter(c -> c == '↔')
            .count();

        assertThat(arrowCount).isEqualTo(1);
    }
}

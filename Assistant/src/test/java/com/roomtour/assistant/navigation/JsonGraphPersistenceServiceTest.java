package com.roomtour.assistant.navigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.config.NavigationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonGraphPersistenceServiceTest {

    @TempDir
    Path tempDir;

    private JsonGraphPersistenceService service;

    @BeforeEach
    void setUp() {
        NavigationProperties props = new NavigationProperties();
        props.setGraphPath(tempDir.resolve("roomgraph.json").toString());
        service = new JsonGraphPersistenceService(props, new ObjectMapper());
    }

    @Test
    void savedGraphRoundTripsThroughJson() {
        RoomGraph original = new RoomGraph();
        original.addConnection("kitchen", "living room", 1.0);
        original.addConnection("living room", "hallway", 1.0);

        service.save(original);
        RoomGraph loaded = service.load().getOrElse(RoomGraph::new);

        assertThat(loaded.getRooms()).containsKey("kitchen");
        assertThat(loaded.getRooms()).containsKey("living room");
        assertThat(loaded.getRooms()).containsKey("hallway");
        assertThat(loaded.getAdjacency().get("kitchen")).containsKey("living room");
        assertThat(loaded.getAdjacency().get("living room")).containsKey("kitchen");
    }

    @Test
    void loadReturnsFailureWhenFileDoesNotExist() {
        assertThat(service.load().isSuccess()).isFalse();
    }

    @Test
    void saveCreatesParentDirectoriesIfNeeded() {
        NavigationProperties props = new NavigationProperties();
        props.setGraphPath(tempDir.resolve("nested/dir/roomgraph.json").toString());
        JsonGraphPersistenceService nested = new JsonGraphPersistenceService(props, new ObjectMapper());

        RoomGraph graph = new RoomGraph();
        graph.addRoom("bedroom");

        assertThat(nested.save(graph).isSuccess()).isTrue();
        assertThat(nested.load().isSuccess()).isTrue();
    }

    @Test
    void roundTripPreservesBidirectionalEdges() {
        RoomGraph original = new RoomGraph();
        original.addConnection("bedroom", "hallway", 1.0);

        service.save(original);
        RoomGraph loaded = service.load().getOrElse(RoomGraph::new);

        assertThat(loaded.getAdjacency().get("bedroom")).containsKey("hallway");
        assertThat(loaded.getAdjacency().get("hallway")).containsKey("bedroom");
    }
}

package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AStarPathfinderTest {

    //        kitchen --- hallway --- bedroom
    //            |                     |
    //        living room           office
    //
    private RoomGraph     graph;
    private AStarPathfinder pathfinder;

    @BeforeEach
    void setUp() {
        graph = new RoomGraph();
        graph.addConnection("kitchen",    "hallway",    1.0);
        graph.addConnection("hallway",    "bedroom",    1.0);
        graph.addConnection("kitchen",    "living room", 1.0);
        graph.addConnection("bedroom",    "office",     1.0);

        RoomGraphHolder holder = new RoomGraphHolder(null) {
            @Override public RoomGraph get() { return graph; }
        };
        pathfinder = new AStarPathfinder(holder);
    }

    @Test
    void findsDirectConnection() {
        List<String> path = pathfinder.findPath("kitchen", "hallway").getOrElse(List::of);
        assertThat(path).containsExactly("kitchen", "hallway");
    }

    @Test
    void findsShortestPathAcrossMultipleHops() {
        List<String> path = pathfinder.findPath("living room", "bedroom").getOrElse(List::of);
        assertThat(path).containsExactly("living room", "kitchen", "hallway", "bedroom");
    }

    @Test
    void findsPathOnFiveRoomGraph() {
        List<String> path = pathfinder.findPath("living room", "office").getOrElse(List::of);
        assertThat(path).containsExactly("living room", "kitchen", "hallway", "bedroom", "office");
    }

    @Test
    void sameRoomReturnsJustThatRoom() {
        List<String> path = pathfinder.findPath("kitchen", "kitchen").getOrElse(List::of);
        assertThat(path).containsExactly("kitchen");
    }

    @Test
    void unknownFromRoomReturnsFailure() {
        AtomicReference<String> msg = new AtomicReference<>("");
        pathfinder.findPath("garage", "bedroom").onFailure(e -> msg.set(e.getMessage()));
        assertThat(msg.get()).containsIgnoringCase("garage");
    }

    @Test
    void unknownToRoomReturnsFailure() {
        AtomicReference<String> msg = new AtomicReference<>("");
        pathfinder.findPath("kitchen", "attic").onFailure(e -> msg.set(e.getMessage()));
        assertThat(msg.get()).containsIgnoringCase("attic");
    }

    @Test
    void unreachableRoomReturnsFailure() {
        graph.addRoom("basement");
        AtomicReference<String> msg = new AtomicReference<>("");
        pathfinder.findPath("kitchen", "basement").onFailure(e -> msg.set(e.getMessage()));
        assertThat(msg.get()).containsIgnoringCase("basement");
    }

    @Test
    void pathUsesDisplayNames() {
        graph.addRoom("Living Room");
        List<String> path = pathfinder.findPath("kitchen", "hallway").getOrElse(List::of);
        assertThat(path).doesNotContain("KITCHEN");
    }

    @Test
    void pathfindingIsBidirectional() {
        List<String> forward = pathfinder.findPath("kitchen", "bedroom").getOrElse(List::of);
        List<String> reverse = pathfinder.findPath("bedroom", "kitchen").getOrElse(List::of);
        assertThat(forward).hasSize(reverse.size());
    }
}

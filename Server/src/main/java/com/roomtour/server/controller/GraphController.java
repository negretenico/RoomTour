package com.roomtour.server.controller;

import com.roomtour.assistant.navigation.RoomGraph;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import com.roomtour.server.model.graph.GraphSnapshot;
import com.roomtour.server.model.graph.RoomEdge;
import com.roomtour.server.model.graph.RoomNode;
import com.roomtour.server.session.RoomSessionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/graph", produces = APPLICATION_JSON_VALUE)
public class GraphController {

    private final RoomGraphHolder graphHolder;
    private final RoomSessionStore roomSessionStore;

    public GraphController(RoomGraphHolder graphHolder, RoomSessionStore roomSessionStore) {
        this.graphHolder      = graphHolder;
        this.roomSessionStore = roomSessionStore;
    }

    @GetMapping
    public ResponseEntity<GraphSnapshot> graph() {
        RoomGraph graph = graphHolder.get();

        List<RoomNode> nodes = graph.getRooms().entrySet().stream()
            .map(e -> new RoomNode(e.getKey(), e.getValue()))
            .toList();

        List<RoomEdge> edges = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        graph.getAdjacency().forEach((from, connections) ->
            connections.keySet().forEach(to -> {
                String pairKey = from.compareTo(to) <= 0 ? from + "|" + to : to + "|" + from;
                if (seen.add(pairKey)) {
                    edges.add(new RoomEdge(pairKey, from, to));
                }
            })
        );

        String currentRoom = roomSessionStore.getOrDefault(RoomSessionStore.GLOBAL_SESSION, null);
        return ResponseEntity.ok(new GraphSnapshot(nodes, edges, currentRoom));
    }
}

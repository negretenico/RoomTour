package com.roomtour.assistant.navigation;

/**
 * GraphBuildingService implementation for autonomous sensor-driven map building.
 * Wraps a fresh RoomGraph; connections are inferred by {@link ExplorationService}
 * as the drone transitions between detected rooms.
 */
public class ExplorationGraphBuilder implements GraphBuildingService {

    private final RoomGraph graph = new RoomGraph();

    @Override
    public void addRoom(String name) {
        graph.addRoom(name);
    }

    @Override
    public void addConnection(String from, String to, double weight) {
        graph.addConnection(from, to, weight);
    }

    @Override
    public RoomGraph getGraph() {
        return graph;
    }
}

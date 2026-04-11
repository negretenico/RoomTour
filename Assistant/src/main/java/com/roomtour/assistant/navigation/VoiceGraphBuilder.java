package com.roomtour.assistant.navigation;

/**
 * GraphBuildingService implementation backed by a mutable RoomGraph.
 * Not a Spring bean — created per session by GraphBuildingServiceFactory.
 */
public class VoiceGraphBuilder implements GraphBuildingService {

    private final RoomGraph graph;

    public VoiceGraphBuilder(RoomGraph graph) {
        this.graph = graph;
    }

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

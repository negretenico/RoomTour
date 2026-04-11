package com.roomtour.assistant.navigation;

/**
 * Stub for future sensor-driven autonomous map building.
 * Will be implemented in a follow-up issue once the drone's
 * odometry and SLAM pipeline is available.
 */
public class ExplorationGraphBuilder implements GraphBuildingService {

    @Override
    public void addRoom(String name) {
        throw new UnsupportedOperationException("Exploration mode not yet implemented");
    }

    @Override
    public void addConnection(String from, String to, double weight) {
        throw new UnsupportedOperationException("Exploration mode not yet implemented");
    }

    @Override
    public RoomGraph getGraph() {
        throw new UnsupportedOperationException("Exploration mode not yet implemented");
    }
}

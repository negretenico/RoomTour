package com.roomtour.assistant.navigation;

public interface GraphBuildingService {
    void addRoom(String name);
    void addConnection(String from, String to, double weight);
    RoomGraph getGraph();
}

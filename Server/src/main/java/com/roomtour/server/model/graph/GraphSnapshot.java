package com.roomtour.server.model.graph;

import java.util.List;

public record GraphSnapshot(List<RoomNode> nodes, List<RoomEdge> edges, String currentRoom) {}

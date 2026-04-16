package com.roomtour.assistant.navigation;

import org.springframework.stereotype.Component;

@Component
public class GraphBuildingServiceFactory {

    public GraphBuildingService create(BuildMode mode) {
        return switch (mode) {
            case VOICE       -> new VoiceGraphBuilder(new RoomGraph());
            case EXPLORATION -> new ExplorationGraphBuilder();
        };
    }

    /** Creates a voice builder pre-seeded with a deep copy of an existing graph. */
    public GraphBuildingService createFrom(RoomGraph existing) {
        return new VoiceGraphBuilder(new RoomGraph(existing));
    }
}

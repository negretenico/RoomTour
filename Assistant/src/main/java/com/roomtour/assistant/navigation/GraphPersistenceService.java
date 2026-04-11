package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;

public interface GraphPersistenceService {
    Try<String>    save(RoomGraph graph);
    Try<RoomGraph> load();
}

package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;

import java.util.List;

public interface PathfindingService {
    /**
     * Returns the ordered list of room display names forming the shortest path
     * from {@code from} to {@code to}, inclusive of both endpoints.
     * Returns a Failure if either room is unknown or no path exists.
     */
    Try<List<String>> findPath(String from, String to);
}

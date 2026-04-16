package com.roomtour.assistant.navigation;

import java.util.*;

/**
 * Mutable domain object representing a home map as a weighted, bidirectional graph.
 * Nodes are rooms; edges are physical connections between rooms.
 * Room names are stored with a normalized key (lowercase, article-stripped) to
 * deduplicate entries like "the Kitchen" and "kitchen".
 */
public class RoomGraph {

    private static final Set<String> ARTICLES = Set.of("the", "a", "an");

    /** normalized key → display name (first-mention wins) */
    private final Map<String, String> rooms = new LinkedHashMap<>();

    /** normalized from → (normalized to → weight), both directions stored */
    private final Map<String, Map<String, Double>> adjacency = new LinkedHashMap<>();

    public RoomGraph() {}

    /** Deep-copies an existing graph so a new editing session doesn't share state. */
    public RoomGraph(RoomGraph source) {
        source.rooms.forEach(this.rooms::put);
        source.adjacency.forEach((from, conns) ->
            conns.forEach((to, w) ->
                this.adjacency.computeIfAbsent(from, k -> new LinkedHashMap<>()).put(to, w)));
    }

    public void addRoom(String name) {
        String key = normalize(name);
        rooms.putIfAbsent(key, name.strip());
    }

    public void addConnection(String from, String to, double weight) {
        addRoom(from);
        addRoom(to);
        String fromKey = normalize(from);
        String toKey   = normalize(to);
        adjacency.computeIfAbsent(fromKey, k -> new LinkedHashMap<>()).put(toKey, weight);
        adjacency.computeIfAbsent(toKey,   k -> new LinkedHashMap<>()).put(fromKey, weight);
    }

    public Map<String, String> getRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public Map<String, Map<String, Double>> getAdjacency() {
        return Collections.unmodifiableMap(adjacency);
    }

    public boolean isEmpty() {
        return rooms.isEmpty();
    }

    /** Human-readable summary suitable for TTS or chat response. */
    public String summary() {
        if (isEmpty()) return "No rooms mapped yet.";

        StringBuilder sb = new StringBuilder();
        sb.append(rooms.size()).append(" room(s): ")
          .append(String.join(", ", rooms.values()));

        Set<String> seen = new LinkedHashSet<>();
        adjacency.forEach((from, connections) ->
            connections.keySet().forEach(to -> {
                String pair = from.compareTo(to) <= 0 ? from + "|" + to : to + "|" + from;
                if (seen.add(pair)) {
                    sb.append("\n  ")
                      .append(rooms.getOrDefault(from, from))
                      .append(" \u2194 ")
                      .append(rooms.getOrDefault(to, to));
                }
            })
        );
        return sb.toString();
    }

    /** Normalizes a room name: lowercase, hyphens to spaces, strips a leading article. */
    public static String normalize(String name) {
        String trimmed = name.strip().toLowerCase().replace('-', ' ');
        for (String article : ARTICLES) {
            if (trimmed.startsWith(article + " ")) {
                return trimmed.substring(article.length()).strip();
            }
        }
        return trimmed;
    }
}

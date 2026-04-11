package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Shortest-path finder operating on the live RoomGraph.
 * Currently uses Dijkstra (h=0) since rooms have no spatial coordinates.
 * When coordinates are added, replace the zero heuristic with Euclidean distance.
 */
@Component
public class AStarPathfinder implements PathfindingService {

    private final RoomGraphHolder holder;

    public AStarPathfinder(RoomGraphHolder holder) {
        this.holder = holder;
    }

    @Override
    public Try<List<String>> findPath(String from, String to) {
        return Try.of(() -> {
            RoomGraph graph   = holder.get();
            String    fromKey = RoomGraph.normalize(from);
            String    toKey   = RoomGraph.normalize(to);

            if (!graph.getRooms().containsKey(fromKey)) {
                throw new IllegalArgumentException("No room called \"" + from + "\" in your map. Check /map for what's mapped.");
            }
            if (!graph.getRooms().containsKey(toKey)) {
                throw new IllegalArgumentException("No room called \"" + to + "\" in your map. Check /map for what's mapped.");
            }
            if (fromKey.equals(toKey)) {
                return List.of(graph.getRooms().get(fromKey));
            }

            Map<String, Double> dist = new HashMap<>();
            Map<String, String> prev = new HashMap<>();

            PriorityQueue<Map.Entry<String, Double>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());

            dist.put(fromKey, 0.0);
            pq.add(Map.entry(fromKey, 0.0));

            while (!pq.isEmpty()) {
                Map.Entry<String, Double> head   = pq.poll();
                String                   current = head.getKey();
                double                   cost    = head.getValue();

                if (cost > dist.getOrDefault(current, Double.MAX_VALUE)) continue;
                if (current.equals(toKey)) break;

                graph.getAdjacency()
                     .getOrDefault(current, Map.of())
                     .forEach((neighbour, weight) -> {
                         double newCost = cost + weight;
                         if (newCost < dist.getOrDefault(neighbour, Double.MAX_VALUE)) {
                             dist.put(neighbour, newCost);
                             prev.put(neighbour, current);
                             pq.add(Map.entry(neighbour, newCost));
                         }
                     });
            }

            if (!prev.containsKey(toKey)) {
                throw new IllegalStateException(
                    "\"" + to + "\" is not reachable from \"" + from + "\". Are they connected in your map?");
            }

            return reconstructPath(prev, graph.getRooms(), fromKey, toKey);
        });
    }

    private List<String> reconstructPath(Map<String, String> prev,
                                          Map<String, String> displayNames,
                                          String fromKey,
                                          String toKey) {
        List<String> path    = new ArrayList<>();
        String       current = toKey;
        while (current != null) {
            path.add(displayNames.getOrDefault(current, current));
            current = prev.get(current);
        }
        Collections.reverse(path);
        return Collections.unmodifiableList(path);
    }
}

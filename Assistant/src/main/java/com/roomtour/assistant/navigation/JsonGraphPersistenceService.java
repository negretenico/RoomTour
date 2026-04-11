package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Try;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.assistant.config.NavigationProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JsonGraphPersistenceService implements GraphPersistenceService {

    private final NavigationProperties props;
    private final ObjectMapper mapper;

    public JsonGraphPersistenceService(NavigationProperties props, ObjectMapper mapper) {
        this.props  = props;
        this.mapper = mapper;
    }

    @Override
    public Try<String> save(RoomGraph graph) {
        return Try.of(() -> {
            Path path = Path.of(props.getGraphPath());
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            String json = mapper.writeValueAsString(GraphDto.from(graph));
            Files.writeString(path, json);
            return "Graph saved to " + path;
        });
    }

    @Override
    public Try<RoomGraph> load() {
        return Try.of(() -> {
            Path path = Path.of(props.getGraphPath());
            String json = Files.readString(path);
            GraphDto dto = mapper.readValue(json, GraphDto.class);
            return dto.toGraph();
        });
    }

    // DTO used only for JSON serialization — not part of the domain.
    static class GraphDto {
        private Map<String, String>              rooms     = new LinkedHashMap<>();
        private Map<String, Map<String, Double>> adjacency = new LinkedHashMap<>();

        static GraphDto from(RoomGraph graph) {
            GraphDto dto = new GraphDto();
            dto.rooms     = new LinkedHashMap<>(graph.getRooms());
            dto.adjacency = new LinkedHashMap<>(graph.getAdjacency());
            return dto;
        }

        RoomGraph toGraph() {
            RoomGraph graph = new RoomGraph();
            rooms.forEach((key, display) -> graph.addRoom(display));
            adjacency.forEach((from, connections) ->
                connections.forEach((to, weight) -> {
                    if (from.compareTo(to) <= 0) {
                        String fromDisplay = rooms.getOrDefault(from, from);
                        String toDisplay   = rooms.getOrDefault(to, to);
                        graph.addConnection(fromDisplay, toDisplay, weight);
                    }
                })
            );
            return graph;
        }

        public Map<String, String>              getRooms()                              { return rooms; }
        public void                             setRooms(Map<String, String> r)         { this.rooms = r; }
        public Map<String, Map<String, Double>> getAdjacency()                          { return adjacency; }
        public void                             setAdjacency(Map<String, Map<String, Double>> a) { this.adjacency = a; }
    }
}

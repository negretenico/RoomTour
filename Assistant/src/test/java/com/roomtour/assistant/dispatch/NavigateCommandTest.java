package com.roomtour.assistant.dispatch;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.dispatch.command.NavigateCommand;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.navigation.PathfindingService;
import com.roomtour.assistant.navigation.RoomGraph;
import com.roomtour.assistant.navigation.RoomGraphHolder;
import com.roomtour.drone.DroneNavigator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavigateCommandTest {

    static final String SESSION = "test-session";

    @Mock PathfindingService    pathfinder;
    @Mock RoomGraphHolder       graphHolder;
    @Mock CurrentRoomRepository roomRepository;
    @Mock DroneNavigator        droneNavigator;

    NavigateCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new NavigateCommand(pathfinder, graphHolder, roomRepository, droneNavigator);
    }

    @Test
    void navigate_callsDroneWithDestinationOnPathfindingSuccess() {
        RoomGraph graph = new RoomGraph();
        graph.addConnection("kitchen", "bedroom", 1.0);
        when(graphHolder.get()).thenReturn(graph);
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("kitchen");
        when(pathfinder.findPath("kitchen", "bedroom"))
            .thenReturn(Try.of(() -> List.of("kitchen", "bedroom")));

        cmd.execute("/navigate bedroom", SESSION);

        verify(droneNavigator).navigate("bedroom");
    }

    @Test
    void navigate_sendsOnlyDestinationNotFullPath() {
        RoomGraph graph = new RoomGraph();
        graph.addConnection("kitchen", "hallway", 1.0);
        graph.addConnection("hallway", "bedroom", 1.0);
        when(graphHolder.get()).thenReturn(graph);
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("kitchen");
        when(pathfinder.findPath("kitchen", "bedroom"))
            .thenReturn(Try.of(() -> List.of("kitchen", "hallway", "bedroom")));

        cmd.execute("/navigate bedroom", SESSION);

        verify(droneNavigator).navigate("bedroom");
    }

    @Test
    void navigate_doesNotCallDroneWhenPathfindingFails() {
        RoomGraph graph = new RoomGraph();
        graph.addRoom("kitchen");
        graph.addRoom("bedroom");
        when(graphHolder.get()).thenReturn(graph);
        when(roomRepository.getCurrentRoom(SESSION)).thenReturn("kitchen");
        when(pathfinder.findPath("kitchen", "bedroom"))
            .thenReturn(Try.of(() -> { throw new RuntimeException("no path found"); }));

        cmd.execute("/navigate bedroom", SESSION);

        verifyNoInteractions(droneNavigator);
    }
}

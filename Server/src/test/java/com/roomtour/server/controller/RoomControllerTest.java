package com.roomtour.server.controller;

import com.roomtour.server.model.room.RoomRequest;
import com.roomtour.server.session.RoomSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock RoomSessionStore roomSessionStore;

    RoomController controller;

    @BeforeEach
    void setUp() {
        controller = new RoomController(roomSessionStore);
    }

    @Test
    void setRoom_storesRoomAndReturnsSessionId() {
        var response = controller.setRoom(new RoomRequest("kitchen", "session-abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sessionId()).isEqualTo("session-abc");
        assertThat(response.getBody().room()).isEqualTo("kitchen");
    }

    @Test
    void setRoom_persistsRoomInStore() {
        controller.setRoom(new RoomRequest("kitchen", "session-abc"));

        verify(roomSessionStore).put("session-abc", "kitchen");
    }

    @Test
    void setRoom_generatesSessionIdWhenNotProvided() {
        var response = controller.setRoom(new RoomRequest("living room", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sessionId()).isNotBlank();
        assertThat(response.getBody().room()).isEqualTo("living room");
    }

    @Test
    void setRoom_returnsBadRequestWhenRoomBlank() {
        var response = controller.setRoom(new RoomRequest("   ", "session-abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setRoom_returnsBadRequestWhenRoomNull() {
        var response = controller.setRoom(new RoomRequest(null, "session-abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

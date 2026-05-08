package com.roomtour.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.server.recognition.RoomDetectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class WebSocketBroadcasterTest {

    private WebSocketBroadcaster broadcaster;
    private WebSocketSession session;

    @BeforeEach
    void setUp() throws Exception {
        broadcaster = new WebSocketBroadcaster(new ObjectMapper());
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        broadcaster.afterConnectionEstablished(session);
    }

    @Test
    void roomDetectedEvent_broadcastsRoomUpdateMessage() throws Exception {
        broadcaster.onRoomDetected(new RoomDetectedEvent(this, "kitchen"));

        var captor = forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        var payload = new ObjectMapper().readTree(captor.getValue().getPayload());
        assertThat(payload.get("type").asText()).isEqualTo("ROOM_UPDATE");
        assertThat(payload.get("room").asText()).isEqualTo("kitchen");
    }
}

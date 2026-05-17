package com.roomtour.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomtour.drone.ExploreCompletedEvent;
import com.roomtour.drone.NavigationCompleted;
import com.roomtour.drone.RoomDetectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class WebSocketBroadcaster extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public WebSocketBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket client disconnected: {}", session.getId());
    }

    @EventListener
    public void onRoomDetected(RoomDetectedEvent event) {
        broadcast(Map.of("type", "ROOM_UPDATE", "room", event.room()));
    }

    @EventListener
    public void onNavigationCompleted(NavigationCompleted event) {
        broadcast(Map.of("type", "NAVIGATION_COMPLETED", "destination", event.destination()));
    }

    @EventListener
    public void onExploreCompleted(ExploreCompletedEvent event) {
        broadcast(Map.of("type", "EXPLORATION_COMPLETE"));
    }

    private void broadcast(Object payload) {
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message", e);
        }
    }
}

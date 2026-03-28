package com.roomtour.server.controller;

import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.server.model.chat.ChatRequest;
import com.roomtour.server.session.RoomSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock CommandRouter commandRouter;
    @Mock RoomSessionStore roomSessionStore;

    ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(commandRouter, roomSessionStore);
    }

    @Test
    void chat_routesMessageAndReturnsResponse() {
        given(commandRouter.route(any())).willReturn(new ButlerResponse("Very good, sir.", "s1"));
        given(roomSessionStore.getOrDefault(any(), any())).willReturn("living room");

        var response = controller.chat(new ChatRequest("Hello Jeeves", "s1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isEqualTo("Very good, sir.");
        assertThat(response.getBody().sessionId()).isEqualTo("s1");
    }

    @Test
    void chat_returnsBadRequestWhenMessageBlank() {
        var response = controller.chat(new ChatRequest("   ", "s1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void chat_returnsBadRequestWhenMessageNull() {
        var response = controller.chat(new ChatRequest(null, "s1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void chat_usesProvidedSessionId() {
        given(commandRouter.route(any())).willReturn(new ButlerResponse("Indeed.", "session-abc"));
        given(roomSessionStore.getOrDefault(any(), any())).willReturn("kitchen");

        controller.chat(new ChatRequest("Hello", "session-abc"));

        var captor = ArgumentCaptor.forClass(ButlerRequest.class);
        verify(commandRouter).route(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo("session-abc");
    }

    @Test
    void chat_generatesSessionIdWhenNotProvided() {
        given(commandRouter.route(any())).willAnswer(inv ->
            new ButlerResponse("Indeed.", inv.getArgument(0, ButlerRequest.class).sessionId()));
        given(roomSessionStore.getOrDefault(any(), any())).willReturn("unknown");

        var response = controller.chat(new ChatRequest("Hello", null));

        assertThat(response.getBody().sessionId()).isNotBlank();
    }

    @Test
    void chat_lookupsRoomFromSessionStore() {
        given(commandRouter.route(any())).willReturn(new ButlerResponse("You are in the kitchen.", "s1"));
        given(roomSessionStore.getOrDefault("s1", "unknown")).willReturn("kitchen");

        controller.chat(new ChatRequest("Where am I?", "s1"));

        var captor = ArgumentCaptor.forClass(ButlerRequest.class);
        verify(commandRouter).route(captor.capture());
        assertThat(captor.getValue().room()).isEqualTo("kitchen");
    }

    @Test
    void chat_defaultsRoomToUnknownWhenSessionHasNoRoom() {
        given(commandRouter.route(any())).willReturn(new ButlerResponse("I'm not sure.", "s1"));
        given(roomSessionStore.getOrDefault("s1", "unknown")).willReturn("unknown");

        controller.chat(new ChatRequest("Where am I?", "s1"));

        var captor = ArgumentCaptor.forClass(ButlerRequest.class);
        verify(commandRouter).route(captor.capture());
        assertThat(captor.getValue().room()).isEqualTo("unknown");
    }
}

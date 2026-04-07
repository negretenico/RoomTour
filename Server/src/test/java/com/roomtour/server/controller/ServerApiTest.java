package com.roomtour.server.controller;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.assistant.lifelog.LifelogService;
import com.roomtour.assistant.lifelog.MutableLifelog;
import com.roomtour.server.model.chat.ChatRequest;
import com.roomtour.server.model.chat.ChatResponse;
import com.roomtour.server.model.lifelog.LifelogResponse;
import com.roomtour.server.model.room.RoomRequest;
import com.roomtour.server.model.room.RoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=local"
)
class ServerApiTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired CommandRouter commandRouter;
    @Autowired LifelogService lifelogService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(commandRouter, lifelogService);
    }

    @Test
    void chat_returnsButlerResponseWithSessionId() {
        given(commandRouter.route(any()))
            .willReturn(new ButlerResponse("Very good, sir.", "session-123"));

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
            "/api/v1/chat",
            new ChatRequest("Hello Jeeves", null),
            ChatResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().response()).isEqualTo("Very good, sir.");
        assertThat(response.getBody().sessionId()).isEqualTo("session-123");
    }

    @Test
    void chat_generatesSessionIdWhenAbsent() {
        given(commandRouter.route(any()))
            .willAnswer(inv -> {
                var req = inv.getArgument(0, com.roomtour.assistant.core.model.ButlerRequest.class);
                return new ButlerResponse("Indeed.", req.sessionId());
            });

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
            "/api/v1/chat",
            new ChatRequest("Hello", null),
            ChatResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sessionId()).isNotBlank();
    }

    @Test
    void chat_returnsBadRequestWhenMessageBlank() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/v1/chat",
            new ChatRequest("", null),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setRoom_storesRoomAndReturnsSessionId() {
        ResponseEntity<RoomResponse> response = restTemplate.postForEntity(
            "/api/v1/room",
            new RoomRequest("kitchen", "session-abc"),
            RoomResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sessionId()).isEqualTo("session-abc");
        assertThat(response.getBody().room()).isEqualTo("kitchen");
    }

    @Test
    void setRoom_generatesSessionIdWhenAbsent() {
        ResponseEntity<RoomResponse> response = restTemplate.postForEntity(
            "/api/v1/room",
            new RoomRequest("living room", null),
            RoomResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sessionId()).isNotBlank();
        assertThat(response.getBody().room()).isEqualTo("living room");
    }

    @Test
    void setRoom_returnsBadRequestWhenRoomBlank() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/v1/room",
            new RoomRequest("", null),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void chat_usesRoomSetByPriorRoomCall() {
        restTemplate.postForEntity(
            "/api/v1/room",
            new RoomRequest("kitchen", "session-xyz"),
            RoomResponse.class
        );

        given(commandRouter.route(any()))
            .willAnswer(inv -> {
                var req = inv.getArgument(0, com.roomtour.assistant.core.model.ButlerRequest.class);
                return new ButlerResponse("You are in the " + req.room(), req.sessionId());
            });

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
            "/api/v1/chat",
            new ChatRequest("Where am I?", "session-xyz"),
            ChatResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isEqualTo("You are in the kitchen");
    }

    @Test
    void lifelog_returnsFormattedSummary() {
        given(lifelogService.formatForPrompt()).willReturn("## Calendar\n- Team standup");

        ResponseEntity<LifelogResponse> response = restTemplate.getForEntity(
            "/api/v1/lifelog",
            LifelogResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().summary()).isEqualTo("## Calendar\n- Team standup");
    }

    @TestConfiguration
    static class MockBeans {

        @Bean
        @Primary
        CommandRouter commandRouter() {
            return Mockito.mock(CommandRouter.class);
        }

        @Bean
        @Primary
        LifelogService lifelogService() {
            return Mockito.mock(LifelogService.class);
        }

        @Bean
        MutableLifelog mutableLifelog() {
            return Mockito.mock(MutableLifelog.class);
        }
    }
}

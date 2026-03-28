package com.roomtour.assistant.chat;

import com.common.functionico.risky.Failure;
import com.common.functionico.risky.Success;
import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.core.model.Message;
import com.roomtour.assistant.lifelog.LifelogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ButlerChatServiceTest {

    @Mock ClaudeClient claudeClient;
    @Mock LifelogService lifelogService;

    private ButlerChatService service;

    @BeforeEach
    void setUp() {
        when(lifelogService.formatForPrompt()).thenReturn("Some lifelog context");
        when(claudeClient.send(anyString(), any())).thenReturn(new Success<>("Hello!"));

        ButlerProperties props = new ButlerProperties();
        props.setName("Jeeves");
        props.setUserName("Nico");
        props.setPersonality("formal butler");
        service = new ButlerChatService(lifelogService, claudeClient, props);
    }

    @Test
    void butlerRepliesWithAIGeneratedResponse() {
        ButlerResponse response = service.chat(new ButlerRequest("Hi", "kitchen", "sess-1"));
        assertThat(response.response()).isEqualTo("Hello!");
    }

    @Test
    void conversationContinuesUnderTheSameSession() {
        ButlerResponse response = service.chat(new ButlerRequest("Hi", "kitchen", "sess-42"));
        assertThat(response.sessionId()).isEqualTo("sess-42");
    }

    @Test
    void newConversationReceivesAUniqueSession() {
        ButlerResponse response = service.chat(new ButlerRequest("Hi", "kitchen", null));
        assertThat(response.sessionId()).isNotBlank();
    }

    @Test
    void butlerKnowsItsNameAndCurrentRoom() {
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        service.chat(new ButlerRequest("Hi", "living room", "sess-1"));
        verify(claudeClient).send(promptCaptor.capture(), any());
        assertThat(promptCaptor.getValue())
            .contains("Jeeves")
            .contains("living room")
            .contains("Nico");
    }

    @Test
    void butlerAcknowledgesWhenItMovesToANewRoom() {
        service.chat(new ButlerRequest("Hello", "kitchen", "sess-move"));
        service.chat(new ButlerRequest("What's in here?", "living room", "sess-move"));

        ArgumentCaptor<List<Message>> historyCaptor = ArgumentCaptor.captor();
        verify(claudeClient, times(2)).send(anyString(), historyCaptor.capture());

        boolean hasMoveMessage = historyCaptor.getAllValues().get(1).stream()
            .anyMatch(m -> m.role().equals("assistant") && m.content().contains("living room"));
        assertThat(hasMoveMessage).isTrue();
    }

    @Test
    void butlerDoesNotAnnounceRoomWhenItHasNotMoved() {
        service.chat(new ButlerRequest("Hello", "kitchen", "sess-same"));
        service.chat(new ButlerRequest("Still here", "kitchen", "sess-same"));

        ArgumentCaptor<List<Message>> historyCaptor = ArgumentCaptor.captor();
        verify(claudeClient, times(2)).send(anyString(), historyCaptor.capture());

        long syntheticMoves = historyCaptor.getAllValues().get(1).stream()
            .filter(m -> m.role().equals("assistant") && m.content().startsWith("[I've moved"))
            .count();
        assertThat(syntheticMoves).isZero();
    }

    @Test
    void longConversationDoesNotGrowUnbounded() {
        for (int i = 0; i < 7; i++) {
            service.chat(new ButlerRequest("Message " + i, "kitchen", "sess-cap"));
        }
        ArgumentCaptor<List<Message>> historyCaptor = ArgumentCaptor.captor();
        verify(claudeClient, atLeastOnce()).send(anyString(), historyCaptor.capture());
        assertThat(historyCaptor.getValue().size()).isLessThanOrEqualTo(ButlerChatService.MAX_TURNS * 2);
    }

    @Test
    void butlerApologisesGracefullyWhenAIIsUnavailable() {
        when(claudeClient.send(anyString(), any())).thenReturn(new Failure<>(new RuntimeException("timeout")));
        ButlerResponse response = service.chat(new ButlerRequest("Hi", "kitchen", "sess-err"));
        assertThat(response.response()).containsIgnoringCase("sorry");
    }
}

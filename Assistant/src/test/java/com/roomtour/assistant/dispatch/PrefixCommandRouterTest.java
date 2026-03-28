package com.roomtour.assistant.dispatch;

import com.common.functionico.risky.Failure;
import com.common.functionico.risky.Success;
import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrefixCommandRouterTest {

    @Mock ChatService<ButlerResponse, ButlerRequest> chatService;
    @Mock LifelogService lifelogService;
    @Mock ClaudeClient claudeClient;

    private PrefixCommandRouter router;

    @BeforeEach
    void setUp() {
        ButlerProperties props = new ButlerProperties();
        props.setName("Jeeves");
        props.setUserName("Nico");
        router = new PrefixCommandRouter(chatService, lifelogService, claudeClient, props);
    }

    @Test
    void plainMessageStartsAConversation() {
        when(chatService.chat(any())).thenReturn(new ButlerResponse("reply", "s1"));
        router.route(new ButlerRequest("What time is it?", "kitchen", "s1"));
        verify(chatService).chat(any());
        verifyNoInteractions(claudeClient);
    }

    @Test
    void commandsBypassConversationHistory() {
        when(lifelogService.formatForPrompt()).thenReturn("context");
        router.route(new ButlerRequest("/whats-new", "kitchen", "s1"));
        verifyNoInteractions(chatService);
    }

    @Test
    void whatsNewShowsCurrentLifelogSummary() {
        when(lifelogService.formatForPrompt()).thenReturn("Team standup at 10am");
        ButlerResponse response = router.route(new ButlerRequest("/whats-new", "kitchen", "s1"));
        assertThat(response.response()).contains("Team standup at 10am");
        verifyNoInteractions(claudeClient);
    }

    @Test
    void whatsNewTellsUserWhenThereIsNothingNew() {
        when(lifelogService.formatForPrompt()).thenReturn("");
        ButlerResponse response = router.route(new ButlerRequest("/whats-new", "kitchen", "s1"));
        assertThat(response.response()).contains("Nothing new");
    }

    @Test
    void dailyBriefingIsNarratedByAI() {
        when(lifelogService.formatForPrompt()).thenReturn("10am standup");
        when(claudeClient.send(anyString(), any())).thenReturn(new Success<>("Good morning, Nico!"));

        ButlerResponse response = router.route(new ButlerRequest("/brief", "kitchen", "s1"));

        assertThat(response.response()).isEqualTo("Good morning, Nico!");
        verify(claudeClient).send(anyString(), any());
    }

    @Test
    void briefingFallsBackGracefullyWhenAIIsUnavailable() {
        when(lifelogService.formatForPrompt()).thenReturn("");
        when(claudeClient.send(anyString(), any())).thenReturn(new Failure<>(new RuntimeException("error")));

        ButlerResponse response = router.route(new ButlerRequest("/brief", "kitchen", "s1"));
        assertThat(response.response()).contains("Unable");
    }

    @Test
    void userCanSaveANoteForLaterContext() {
        ButlerResponse response = router.route(new ButlerRequest("/add-note Call mom back", "kitchen", "s1"));
        verify(lifelogService).addNote("Call mom back");
        assertThat(response.response()).contains("Call mom back");
    }

    @Test
    void addNoteWithoutTextIsRejected() {
        ButlerResponse response = router.route(new ButlerRequest("/add-note", "kitchen", "s1"));
        assertThat(response.response()).containsIgnoringCase("unknown");
    }

    @Test
    void unrecognisedCommandIsReportedToUser() {
        ButlerResponse response = router.route(new ButlerRequest("/fly-to-moon", "kitchen", "s1"));
        assertThat(response.response()).containsIgnoringCase("unknown");
        assertThat(response.response()).contains("/fly-to-moon");
    }

    @Test
    void commandPreservesTheActiveSession() {
        when(lifelogService.formatForPrompt()).thenReturn("ctx");
        ButlerResponse response = router.route(new ButlerRequest("/whats-new", "kitchen", "my-session"));
        assertThat(response.sessionId()).isEqualTo("my-session");
    }
}

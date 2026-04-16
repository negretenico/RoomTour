package com.roomtour.assistant.dispatch;

import com.roomtour.assistant.chat.ChatService;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.command.ButlerCommand;
import com.roomtour.assistant.dispatch.command.MapCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrefixCommandRouterTest {

    @Mock ChatService<ButlerResponse, ButlerRequest> chatService;
    @Mock MapCommand mapCommand;
    @Mock ButlerCommand commandA;
    @Mock ButlerCommand commandB;

    private PrefixCommandRouter router;

    @BeforeEach
    void setUp() {
        when(commandA.token()).thenReturn("/alpha");
        when(commandB.token()).thenReturn("/beta");
        when(commandA.intentPattern()).thenReturn(Optional.empty());
        when(commandB.intentPattern()).thenReturn(Optional.empty());
        router = new PrefixCommandRouter(chatService, List.of(commandA, commandB), mapCommand);
    }

    @Test
    void plainMessageDelegatesToChatService() {
        when(chatService.chat(any())).thenReturn(new ButlerResponse("reply", "s1"));
        router.route(new ButlerRequest("hello", null, "s1"));
        verify(chatService).chat(any());
        verify(commandA, never()).execute(anyString(), anyString());
        verify(commandB, never()).execute(anyString(), anyString());
    }

    @Test
    void knownCommandTokenDispatchesToMatchingCommand() {
        when(commandA.execute(anyString(), anyString())).thenReturn(new ButlerResponse("ok", "s1"));
        ButlerResponse response = router.route(new ButlerRequest("/alpha", null, "s1"));
        verify(commandA).execute("/alpha", "s1");
        assertThat(response.response()).isEqualTo("ok");
        verifyNoInteractions(chatService);
    }

    @Test
    void unknownCommandReturnsErrorMessage() {
        ButlerResponse response = router.route(new ButlerRequest("/unknown-cmd", null, "s1"));
        assertThat(response.response()).containsIgnoringCase("Unknown command").contains("/unknown-cmd");
        verifyNoInteractions(chatService);
    }

    @Test
    void commandsListsUsageOfAllRegisteredCommands() {
        when(commandA.usage()).thenReturn("/alpha");
        when(commandB.usage()).thenReturn("/beta <arg>");
        ButlerResponse response = router.route(new ButlerRequest("/commands", null, "s1"));
        assertThat(response.response()).contains("/alpha").contains("/beta <arg>");
        verifyNoInteractions(chatService);
    }

    @Test
    void sessionIdIsPreservedInResponse() {
        when(commandA.execute(anyString(), anyString())).thenReturn(new ButlerResponse("ok", "my-session"));
        ButlerResponse response = router.route(new ButlerRequest("/alpha", null, "my-session"));
        assertThat(response.sessionId()).isEqualTo("my-session");
    }

    @Test
    void activeMapSessionInterceptsFreeText() {
        when(mapCommand.isSessionActive("s1")).thenReturn(true);
        when(mapCommand.handleFreeText(anyString(), anyString())).thenReturn(new ButlerResponse("mapped", "s1"));
        ButlerResponse response = router.route(new ButlerRequest("kitchen to living room", null, "s1"));
        verify(mapCommand).handleFreeText("kitchen to living room", "s1");
        assertThat(response.response()).isEqualTo("mapped");
        verifyNoInteractions(chatService);
    }

    @Test
    void activeMapSessionDoesNotInterceptSlashCommands() {
        when(mapCommand.isSessionActive("s1")).thenReturn(true);
        when(commandA.execute(anyString(), anyString())).thenReturn(new ButlerResponse("ok", "s1"));
        router.route(new ButlerRequest("/alpha", null, "s1"));
        verify(commandA).execute("/alpha", "s1");
        verify(mapCommand, never()).handleFreeText(anyString(), anyString());
    }
}

package com.roomtour.voice;

import com.common.functionico.risky.Try;
import com.roomtour.assistant.core.model.ButlerRequest;
import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.dispatch.CommandRouter;
import com.roomtour.voice.stt.MicCapture;
import com.roomtour.voice.stt.SpeechToText;
import com.roomtour.voice.tts.AudioPlayer;
import com.roomtour.voice.tts.TextToSpeech;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceLoopTest {

    @Mock MicCapture    micCapture;
    @Mock SpeechToText  stt;
    @Mock CommandRouter commandRouter;
    @Mock TextToSpeech  tts;
    @Mock AudioPlayer   audioPlayer;

    @Test
    void fullPipelineFlowsFromMicToSpeaker() throws InterruptedException {
        AudioChunk inputChunk  = new AudioChunk(new byte[]{1, 2, 3, 4}, 16000);
        AudioChunk outputChunk = new AudioChunk(new byte[]{5, 6, 7, 8}, 22050);
        CountDownLatch played  = new CountDownLatch(1);

        when(micCapture.capture()).thenReturn(Try.of(() -> inputChunk));
        when(stt.transcribe(inputChunk)).thenReturn(Try.of(() -> "hello"));
        when(commandRouter.route(any(ButlerRequest.class)))
                .thenReturn(new ButlerResponse("hello back", "session-1"));
        when(tts.synthesize("hello back")).thenReturn(Try.of(() -> outputChunk));
        doAnswer(inv -> { played.countDown(); return null; })
                .when(audioPlayer).play(outputChunk);

        VoiceLoop loop = new VoiceLoop(micCapture, stt, commandRouter, tts, audioPlayer);
        loop.start();

        assertThat(played.await(5, TimeUnit.SECONDS)).isTrue();
        loop.stop();

        verify(audioPlayer, atLeastOnce()).play(outputChunk);
    }

    @Test
    void micFailureDoesNotCrashLoop() throws InterruptedException {
        CountDownLatch secondAttempt = new CountDownLatch(2);

        when(micCapture.capture()).thenAnswer(inv -> {
            secondAttempt.countDown();
            return Try.of(() -> { throw new RuntimeException("mic error"); });
        });

        VoiceLoop loop = new VoiceLoop(micCapture, stt, commandRouter, tts, audioPlayer);
        loop.start();

        assertThat(secondAttempt.await(5, TimeUnit.SECONDS)).isTrue();
        loop.stop();

        verifyNoInteractions(stt, tts, audioPlayer);
    }
}

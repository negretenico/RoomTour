package com.roomtour.server.controller;

import com.common.functionico.value.Maybe;
import com.roomtour.recognition.classify.RoomClassifier;
import com.roomtour.recognition.core.model.DetectedObject;
import com.roomtour.recognition.core.model.RoomObservation;
import com.roomtour.recognition.core.model.SidecarPayload;
import com.roomtour.server.session.RoomSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecognitionControllerTest {

    @Mock RoomClassifier roomClassifier;
    @Mock RoomSessionStore roomSessionStore;

    private RecognitionController controller;

    @BeforeEach
    void setUp() {
        controller = new RecognitionController(roomClassifier, roomSessionStore);
    }

    @Test
    void webhookUpdatesSessionWhenRoomIsRecognised() {
        when(roomClassifier.classify(any())).thenReturn(Maybe.of("kitchen"));
        var payload = new SidecarPayload(List.of(new DetectedObject("stove", 0.92)));

        var response = controller.onSidecarPush(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(roomSessionStore).put(RecognitionController.GLOBAL_SESSION, "kitchen");
    }

    @Test
    void webhookDoesNotUpdateSessionWhenClassifierIsInconclusive() {
        when(roomClassifier.classify(any())).thenReturn(Maybe.none());
        var payload = new SidecarPayload(List.of());

        controller.onSidecarPush(payload);

        verify(roomSessionStore, never()).put(any(), any());
    }

    @Test
    void webhookPassesDetectedObjectsToClassifier() {
        when(roomClassifier.classify(any())).thenReturn(Maybe.none());
        var objects = List.of(new DetectedObject("sofa", 0.88));
        var payload = new SidecarPayload(objects);

        controller.onSidecarPush(payload);

        ArgumentCaptor<RoomObservation> captor = ArgumentCaptor.forClass(RoomObservation.class);
        verify(roomClassifier).classify(captor.capture());
        assertThat(captor.getValue().detectedObjects()).isEqualTo(objects);
    }
}

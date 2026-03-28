package com.roomtour.server.controller;

import com.roomtour.assistant.lifelog.LifelogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LifelogControllerTest {

    @Mock LifelogService lifelogService;

    LifelogController controller;

    @BeforeEach
    void setUp() {
        controller = new LifelogController(lifelogService);
    }

    @Test
    void lifelog_returnsFormattedSummaryFromService() {
        given(lifelogService.formatForPrompt()).willReturn("## Calendar\n- Team standup");

        var response = controller.lifelog();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().summary()).isEqualTo("## Calendar\n- Team standup");
    }

    @Test
    void lifelog_returnsEmptySummaryWhenNoLifelogData() {
        given(lifelogService.formatForPrompt()).willReturn("");

        var response = controller.lifelog();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().summary()).isEmpty();
    }
}

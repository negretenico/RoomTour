package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.core.model.CalendarEvent;
import com.roomtour.assistant.core.model.HealthData;
import com.roomtour.assistant.core.model.WeatherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLifelogTest {

    private InMemoryLifelog lifelog;

    @BeforeEach
    void setUp() {
        lifelog = new InMemoryLifelog(
            List.of(new CalendarEvent("2026-03-27", "10:00 AM", "Team standup")),
            new WeatherSnapshot("Austin, TX", "Sunny", 72, 78, 55, "Clear skies"),
            new HealthData(7.5, "good", 4200, 10000, "Feeling great")
        );
    }

    @Test
    void butlerCanSeeUpcomingCalendarEvents() {
        String output = lifelog.formatForPrompt();
        assertThat(output).contains("Team standup");
        assertThat(output).contains("2026-03-27");
        assertThat(output).contains("10:00 AM");
    }

    @Test
    void butlerKnowsCurrentWeather() {
        String output = lifelog.formatForPrompt();
        assertThat(output).contains("Austin, TX");
        assertThat(output).contains("Sunny");
        assertThat(output).contains("72.0°F");
    }

    @Test
    void butlerKnowsUserHealthMetrics() {
        String output = lifelog.formatForPrompt();
        assertThat(output).contains("7.5 hours");
        assertThat(output).contains("good");
        assertThat(output).contains("4200");
    }

    @Test
    void manualNoteIsImmediatelyAvailableToButler() {
        lifelog.addNote("Call mom back");
        assertThat(lifelog.formatForPrompt()).contains("Call mom back");
    }

    @Test
    void allManualNotesAreRetainedInContext() {
        lifelog.addNote("Buy groceries");
        lifelog.addNote("Review PR");
        String output = lifelog.formatForPrompt();
        assertThat(output).contains("Buy groceries");
        assertThat(output).contains("Review PR");
    }

    @Test
    void newCalendarEventIsImmediatelyVisibleToButler() {
        lifelog.addCalendarEvent(new CalendarEvent("2026-03-28", "2:00 PM", "Doctor appointment"));
        assertThat(lifelog.formatForPrompt()).contains("Doctor appointment");
    }

    @Test
    void butlerContextIsEmptyWhenNoLifelogDataExists() {
        InMemoryLifelog empty = new InMemoryLifelog(List.of(), null, null);
        assertThat(empty.formatForPrompt()).isBlank();
    }
}

package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.core.model.CalendarEvent;
import com.roomtour.assistant.core.model.HealthData;
import com.roomtour.assistant.core.model.WeatherSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory lifelog seeded from config at startup.
 * All write methods are thread-safe — safe to call from a scheduled worker.
 * State is lost on restart by design.
 */
public class InMemoryLifelog implements LifelogService, MutableLifelog {

    private final List<CalendarEvent> calendar;
    private volatile WeatherSnapshot  weather;
    private final HealthData          health;
    private final List<String>        notes = new CopyOnWriteArrayList<>();

    public InMemoryLifelog(List<CalendarEvent> calendar, WeatherSnapshot weather, HealthData health) {
        this.calendar = new CopyOnWriteArrayList<>(calendar);
        this.weather  = weather;
        this.health   = health;
    }

    @Override
    public void addNote(String note) {
        notes.add(note);
    }

    @Override
    public void addCalendarEvent(CalendarEvent event) {
        calendar.add(event);
    }

    @Override
    public String formatForPrompt() {
        return Stream.of(calendarSection(), weatherSection(), healthSection(), notesSection())
            .flatMap(Optional::stream)
            .collect(Collectors.joining("\n"));
    }

    @Override
    public void updateWeather(WeatherSnapshot snapshot) {
        this.weather = snapshot;
    }

    // ---------------------------------------------------------------------------
    // Pure section formatters — each returns empty when there is nothing to show
    // ---------------------------------------------------------------------------

    private Optional<String> calendarSection() {
        return calendar.isEmpty() ? Optional.empty() : Optional.of(
            "Upcoming calendar events:\n" +
            calendar.stream()
                .map(e -> "  - " + e.date() + " at " + e.time() + ": " + e.title())
                .collect(Collectors.joining("\n"))
        );
    }

    private Optional<String> weatherSection() {
        return Optional.ofNullable(weather).map(w ->
            "Weather in " + w.location() + ":\n" +
            "  " + w.condition() + ", " + w.temperatureF() + "°F" +
            " (high " + w.highF() + "°F / low " + w.lowF() + "°F)" +
            Optional.ofNullable(w.forecast())
                .filter(f -> !f.isBlank())
                .map(f -> "\n  Forecast: " + f)
                .orElse("")
        );
    }

    private Optional<String> healthSection() {
        return Optional.ofNullable(health).map(h ->
            "Health data:\n" +
            "  Sleep: " + h.sleepHours() + " hours" +
            Optional.ofNullable(h.sleepQuality())
                .filter(q -> !q.isBlank())
                .map(q -> " (" + q + ")")
                .orElse("") + "\n" +
            "  Steps: " + h.stepsToday() + " / " + h.stepsGoal() + " goal" +
            Optional.ofNullable(h.notes())
                .filter(n -> !n.isBlank())
                .map(n -> "\n  Note: " + n)
                .orElse("")
        );
    }

    private Optional<String> notesSection() {
        return notes.isEmpty() ? Optional.empty() : Optional.of(
            "Your notes:\n" +
            notes.stream()
                .map(n -> "  - " + n)
                .collect(Collectors.joining("\n"))
        );
    }
}

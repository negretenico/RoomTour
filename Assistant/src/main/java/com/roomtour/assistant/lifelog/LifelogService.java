package com.roomtour.assistant.lifelog;

import com.roomtour.assistant.core.model.CalendarEvent;

public interface LifelogService {
    void addNote(String note);
    /** Thread-safe — safe to call from a scheduled worker. */
    void addCalendarEvent(CalendarEvent event);
    String formatForPrompt();
}

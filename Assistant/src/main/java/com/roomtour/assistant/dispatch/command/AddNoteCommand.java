package com.roomtour.assistant.dispatch.command;

import com.roomtour.assistant.core.model.ButlerResponse;
import com.roomtour.assistant.lifelog.LifelogService;
import org.springframework.stereotype.Component;

@Component
public class AddNoteCommand implements ButlerCommand {

    private final LifelogService lifelogService;

    public AddNoteCommand(LifelogService lifelogService) {
        this.lifelogService = lifelogService;
    }

    @Override public String token() { return "/add-note"; }
    @Override public String usage() { return "/add-note <text>"; }

    @Override
    public ButlerResponse execute(String message, String sessionId) {
        String note = message.substring(token().length()).strip();
        if (note.isBlank()) {
            return new ButlerResponse("Unknown command: /add-note", sessionId);
        }
        lifelogService.addNote(note);
        return new ButlerResponse("Got it, I've noted: \"" + note + "\"", sessionId);
    }
}

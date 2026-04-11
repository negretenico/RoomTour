package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Failure;
import com.common.functionico.risky.Success;
import com.common.functionico.risky.Try;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Stateless parser that extracts room connections from natural language utterances
 * and applies them to a GraphBuildingService.
 *
 * Supports patterns like:
 *   "kitchen connects to the living room"
 *   "hallway leads to bedroom"
 *   "office is next to the library"
 */
@Component
public class ConnectionPatternParser {

    private static final List<String> CONNECTION_VERBS = List.of(
        "is connected to",
        "connects to",
        "is adjacent to",
        "is next to",
        "leads to",
        "goes to"
    );

    private static final Set<String> ARTICLES = Set.of("the", "a", "an");

    public Try<String> parse(String utterance, GraphBuildingService service) {
        String lower = utterance.strip().toLowerCase();

        for (String verb : CONNECTION_VERBS) {
            int idx = lower.indexOf(verb);
            if (idx < 0) continue;

            String fromRaw = lower.substring(0, idx).strip();
            String toRaw   = lower.substring(idx + verb.length()).strip();

            if (fromRaw.isBlank() || toRaw.isBlank()) continue;

            String from = stripArticle(fromRaw);
            String to   = stripArticle(toRaw);

            if (from.isBlank() || to.isBlank()) continue;

            service.addConnection(from, to, 1.0);

            String fromDisplay = service.getGraph().getRooms()
                .getOrDefault(RoomGraph.normalize(from), from);
            String toDisplay = service.getGraph().getRooms()
                .getOrDefault(RoomGraph.normalize(to), to);

            return new Success<>("Got it \u2014 " + fromDisplay + " \u2194 " + toDisplay + " connected.");
        }

        return new Failure<>(new IllegalArgumentException(
            "Could not parse room connection from: \"" + utterance + "\". " +
            "Try: 'kitchen connects to the living room'."
        ));
    }

    private static String stripArticle(String s) {
        for (String article : ARTICLES) {
            if (s.startsWith(article + " ")) {
                return s.substring(article.length()).strip();
            }
        }
        return s;
    }
}

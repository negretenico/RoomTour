package com.roomtour.assistant.navigation;

import com.common.functionico.risky.Failure;
import com.common.functionico.risky.Success;
import com.common.functionico.risky.Try;
import com.common.functionico.value.Maybe;
import com.roomtour.assistant.config.NavigationProperties;
import org.springframework.stereotype.Component;

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

    private final NavigationProperties props;

    public ConnectionPatternParser(NavigationProperties props) {
        this.props = props;
    }

    public Try<String> parse(String utterance, GraphBuildingService service) {
        String lower = utterance.strip().toLowerCase();

        return Maybe.of(
                props.getConnectionVerbs().stream()
                    .filter(lower::contains)
                    .findFirst()
                    .orElse(null))
            .map(verb -> buildConnection(lower, verb, service))
            .orElse(new Failure<>(new IllegalArgumentException(
                "Could not parse room connection from: \"" + utterance + "\". " +
                "Try: 'kitchen connects to the living room'."
            )));
    }

    private Try<String> buildConnection(String lower, String verb, GraphBuildingService service) {
        int    idx     = lower.indexOf(verb);
        String from    = stripArticle(lower.substring(0, idx).strip());
        String to      = stripArticle(lower.substring(idx + verb.length()).strip());

        if (from.isBlank() || to.isBlank()) {
            return new Failure<>(new IllegalArgumentException("Room name missing on one side of: \"" + lower + "\""));
        }

        service.addConnection(from, to, 1.0);

        String fromDisplay = service.getGraph().getRooms().getOrDefault(RoomGraph.normalize(from), from);
        String toDisplay   = service.getGraph().getRooms().getOrDefault(RoomGraph.normalize(to), to);

        return new Success<>("Got it \u2014 " + fromDisplay + " \u2194 " + toDisplay + " connected.");
    }

    private String stripArticle(String s) {
        return props.getArticles().stream()
            .filter(a -> s.startsWith(a + " "))
            .findFirst()
            .map(a -> s.substring(a.length()).strip())
            .orElse(s);
    }
}

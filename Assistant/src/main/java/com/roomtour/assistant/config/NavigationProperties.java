package com.roomtour.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "roomtour.navigation")
public class NavigationProperties {

    private String graphPath            = "./data/roomgraph.json";
    private int    sessionTimeoutMinutes = 5;
    private String mapDoneKeyword       = "done";
    private String mapPrompt            =
        "No map yet. Start describing your home \u2014 say 'kitchen connects to the living room'. " +
        "Say 'done' when finished.";

    private List<String> connectionVerbs = new ArrayList<>(List.of(
        "is connected to",
        "connects to",
        "is adjacent to",
        "is next to",
        "leads to",
        "goes to"
    ));

    private List<String> articles = new ArrayList<>(List.of("the", "a", "an"));
}

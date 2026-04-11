package com.roomtour.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roomtour.navigation")
public class NavigationProperties {

    private String graphPath = "./data/roomgraph.json";

    public String getGraphPath()              { return graphPath; }
    public void   setGraphPath(String path)   { this.graphPath = path; }
}

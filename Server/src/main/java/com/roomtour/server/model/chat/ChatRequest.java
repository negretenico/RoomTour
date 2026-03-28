package com.roomtour.server.model.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequest(String message, @JsonProperty("sessionId") String sessionId) {}

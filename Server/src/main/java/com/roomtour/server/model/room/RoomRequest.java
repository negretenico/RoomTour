package com.roomtour.server.model.room;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomRequest(String room, @JsonProperty("sessionId") String sessionId) {}

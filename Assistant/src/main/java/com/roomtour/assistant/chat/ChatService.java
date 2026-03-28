package com.roomtour.assistant.chat;

public interface ChatService<T, R> {
    T chat(R request);
}

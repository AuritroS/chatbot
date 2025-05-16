package com.example.chatbot;

public class ChatRequest {
    private String userMessage;

    public ChatRequest(String message) {
        this.userMessage = message;
    }

    public String getMessage() {
        return userMessage;
    }
}

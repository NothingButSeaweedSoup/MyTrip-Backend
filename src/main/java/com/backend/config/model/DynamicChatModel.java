package com.backend.config.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicChatModel implements ChatModel {

    private final AtomicReference<ChatModel> delegate;

    public DynamicChatModel(ChatModel initial) {
        this.delegate = new AtomicReference<>(initial);
    }

    public void swap(ChatModel newModel) {
        this.delegate.set(newModel);
    }

    @Override
    public String chat(String userMessage) {
        return delegate.get().chat(userMessage);
    }

    @Override
    public ChatResponse chat(ChatMessage... messages) {
        return delegate.get().chat(messages);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        return delegate.get().chat(messages);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return delegate.get().chat(request);
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        return delegate.get().doChat(request);
    }
}

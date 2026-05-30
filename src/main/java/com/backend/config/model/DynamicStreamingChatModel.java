package com.backend.config.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicStreamingChatModel implements StreamingChatModel {

    private final AtomicReference<StreamingChatModel> delegate;

    public DynamicStreamingChatModel(StreamingChatModel initial) {
        this.delegate = new AtomicReference<>(initial);
    }

    public void swap(StreamingChatModel newModel) {
        this.delegate.set(newModel);
    }

    @Override
    public void chat(String userMessage, StreamingChatResponseHandler handler) {
        delegate.get().chat(userMessage, handler);
    }

    @Override
    public void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        delegate.get().chat(messages, handler);
    }

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        delegate.get().chat(request, handler);
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        delegate.get().doChat(request, handler);
    }
}

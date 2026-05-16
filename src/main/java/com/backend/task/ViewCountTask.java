package com.backend.task;

import com.backend.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ViewCountTask {

    @Autowired
    private PostMapper postMapper;

    @Async
    public void incrementViews(Long postId) {
        postMapper.incrementViews(postId);
    }
}

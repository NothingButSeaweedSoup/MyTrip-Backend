package com.backend.service.impl;

import com.backend.dto.PostVO;
import com.backend.entity.Post;
import com.backend.entity.UserBehavior;
import com.backend.service.PostService;
import com.backend.service.RecommendService;
import com.backend.service.UserBehaviorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private PostService postService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Override
    public IPage<PostVO> getFeed(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .last("ORDER BY RAND()");
        Page<Post> postPage = postService.page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(p -> postService.toPostVO(p, userId));
    }

    @Override
    public void reportBehavior(Long userId, Long postId, String actionType, Integer duration) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setPostId(postId);
        behavior.setActionType(actionType);
        behavior.setDuration(duration);
        userBehaviorService.save(behavior);
    }
}

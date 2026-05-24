package com.backend.task;

import com.backend.common.RedisKeys;
import com.backend.entity.Comment;
import com.backend.entity.Post;
import com.backend.service.CommentService;
import com.backend.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CounterSyncTask {

    private static final Logger log = LoggerFactory.getLogger(CounterSyncTask.class);

    // Redis key constants → com.backend.common.RedisKeys

    /** 最少间隔 30 秒 */
    private static final long MIN_INTERVAL_MS = 30_000;
    /** 脏数据达到 50 条立刻同步 */
    private static final int FLUSH_THRESHOLD = 50;

    private long lastPostSyncAt;
    private long lastCommentSyncAt;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Scheduled(fixedDelay = 5_000)
    public void syncPosts() {
        try {
            Long size = stringRedisTemplate.opsForSet().size(RedisKeys.DIRTY_POSTS);
            if (size == null || size == 0) return;

            long now = System.currentTimeMillis();
            if (size < FLUSH_THRESHOLD && now - lastPostSyncAt < MIN_INTERVAL_MS) {
                return;
            }

            Set<String> dirtyIds = stringRedisTemplate.opsForSet().members(RedisKeys.DIRTY_POSTS);
            if (dirtyIds == null || dirtyIds.isEmpty()) return;

            for (String idStr : dirtyIds) {
                Long postId = Long.parseLong(idStr);
                Post post = postService.getById(postId);
                if (post == null) {
                    stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_POSTS, idStr);
                    continue;
                }

                boolean changed = false;

                String likeCount = stringRedisTemplate.opsForValue().get(RedisKeys.POST_LIKES + postId);
                if (likeCount != null) {
                    long likes = Long.parseLong(likeCount);
                    if (likes != post.getLikes()) {
                        post.setLikes(likes);
                        changed = true;
                    }
                }

                String viewCount = stringRedisTemplate.opsForValue().get(RedisKeys.POST_VIEWS + postId);
                if (viewCount != null) {
                    long views = Long.parseLong(viewCount);
                    if (views != post.getViews()) {
                        post.setViews(views);
                        changed = true;
                    }
                }

                if (changed) {
                    postService.updateById(post);
                }
                stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_POSTS, idStr);
            }
            lastPostSyncAt = now;
        } catch (Exception e) {
            log.warn("syncPosts failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5_000)
    public void syncComments() {
        try {
            Long size = stringRedisTemplate.opsForSet().size(RedisKeys.DIRTY_COMMENTS);
            if (size == null || size == 0) return;

            long now = System.currentTimeMillis();
            if (size < FLUSH_THRESHOLD && now - lastCommentSyncAt < MIN_INTERVAL_MS) {
                return;
            }

            Set<String> dirtyIds = stringRedisTemplate.opsForSet().members(RedisKeys.DIRTY_COMMENTS);
            if (dirtyIds == null || dirtyIds.isEmpty()) return;

            for (String idStr : dirtyIds) {
                Long commentId = Long.parseLong(idStr);
                Comment comment = commentService.getById(commentId);
                if (comment == null) {
                    stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_COMMENTS, idStr);
                    continue;
                }

                String likeCount = stringRedisTemplate.opsForValue().get(RedisKeys.COMMENT_LIKES + commentId);
                if (likeCount != null) {
                    long likes = Long.parseLong(likeCount);
                    if (likes != comment.getLikes()) {
                        comment.setLikes(likes);
                        commentService.updateById(comment);
                    }
                }
                stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_COMMENTS, idStr);
            }
            lastCommentSyncAt = now;
        } catch (Exception e) {
            log.warn("syncComments failed: {}", e.getMessage());
        }
    }
}

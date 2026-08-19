package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.RedisKeys;
import com.backend.common.UnauthorizedException;
import com.backend.dto.CommentCreateRequest;
import com.backend.dto.CommentVO;
import com.backend.entity.Comment;
import com.backend.entity.Post;
import com.backend.entity.User;
import com.backend.mapper.CommentMapper;
import com.backend.mapper.UserMapper;
import com.backend.entity.UserBehavior;
import com.backend.service.CommentService;
import com.backend.service.PostService;
import com.backend.service.UserBehaviorService;
import com.backend.util.ImageUrlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    // Redis key constants → com.backend.common.RedisKeys

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ImageUrlUtil imageUrlUtil;

    @Autowired
    private PostService postService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Override
    @Transactional
    public Long createComment(Long userId, CommentCreateRequest request) {
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setAuthorId(userId);
        comment.setParentCommentId(request.getParentCommentId());
        comment.setContent(request.getContent());
        comment.setLikes(0L);
        comment.setStatus(0); // 正常
        save(comment);

        // 更新帖子评论数
        Post post = postService.getById(request.getPostId());
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postService.updateById(post);
        }

        // 写入用户行为记录（评论）
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setPostId(request.getPostId());
        ub.setActionType("comment");
        ub.setCreateTime(new Date());
        userBehaviorService.save(ub);

        return comment.getCommentId();
    }

    @Override
    public List<CommentVO> getCommentTree(Long postId) {
        return getCommentTree(postId, null);
    }

    @Override
    public List<CommentVO> getCommentTree(Long postId, Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
               .eq(Comment::getStatus, 0) // 仅正常
               .orderByAsc(Comment::getCreateTime);

        List<Comment> comments = list(wrapper);
        Map<Long, User> userMap = loadUserMap(comments);

        // 分离顶级评论和回复
        Map<Long, List<Comment>> parentMap = new HashMap<>();
        List<Comment> roots = new ArrayList<>();

        for (Comment c : comments) {
            if (c.getParentCommentId() == null) {
                roots.add(c);
            } else {
                parentMap.computeIfAbsent(c.getParentCommentId(), k -> new ArrayList<>()).add(c);
            }
        }

        // 递归构建树
        return roots.stream()
                .map(root -> buildCommentVO(root, parentMap, userMap, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null || comment.getStatus() == 2) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("无权删除此评论");
        }
        comment.setStatus(2);
        updateById(comment);
    }

    @Override
    public long toggleLike(Long userId, Long commentId, String action) {
        try {
            return toggleLikeWithRedis(userId, commentId, action);
        } catch (Exception e) {
            return toggleLikeWithDb(commentId, action);
        }
    }

    private long toggleLikeWithRedis(Long userId, Long commentId, String action) {
        String likeKey = RedisKeys.COMMENT_LIKES + commentId;
        String likedSetKey = RedisKeys.COMMENT_LIKED_SET + commentId;

        if ("like".equals(action)) {
            Long added = stringRedisTemplate.opsForSet().add(likedSetKey, userId.toString());
            if (added != null && added > 0) {
                saveCommentLikeRecord(userId, commentId);
                seedCounterFromDb(likeKey, commentId);
                long count = stringRedisTemplate.opsForValue().increment(likeKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_COMMENTS, commentId.toString());
                return count;
            }
        } else if ("unlike".equals(action)) {
            Long removed = stringRedisTemplate.opsForSet().remove(likedSetKey, userId.toString());
            if (removed != null && removed > 0) {
                removeCommentLikeRecord(userId, commentId);
                seedCounterFromDb(likeKey, commentId);
                long count = stringRedisTemplate.opsForValue().decrement(likeKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_COMMENTS, commentId.toString());
                return count;
            }
        }
        String count = stringRedisTemplate.opsForValue().get(likeKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void seedCounterFromDb(String key, Long commentId) {
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            Comment comment = getById(commentId);
            if (comment != null) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(comment.getLikes()));
            }
        }
    }

    private long toggleLikeWithDb(Long commentId, String action) {
        Comment comment = getById(commentId);
        if (comment == null) return 0;

        if ("like".equals(action)) {
            comment.setLikes(comment.getLikes() + 1);
        } else if ("unlike".equals(action)) {
            comment.setLikes(Math.max(0, comment.getLikes() - 1));
        }
        updateById(comment);
        return comment.getLikes();
    }

    private boolean isLikedByUser(Long commentId, Long userId) {
        try {
            return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeys.COMMENT_LIKED_SET + commentId, userId.toString())
            );
        } catch (Exception e) {
            return hasCommentLikeRecord(userId, commentId);
        }
    }

    private boolean hasCommentLikeRecord(Long userId, Long commentId) {
        return userBehaviorService.lambdaQuery()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getCommentId, commentId)
                .eq(UserBehavior::getActionType, "like")
                .count() > 0;
    }

    private void saveCommentLikeRecord(Long userId, Long commentId) {
        if (hasCommentLikeRecord(userId, commentId)) return;
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setCommentId(commentId);
        ub.setActionType("like");
        userBehaviorService.save(ub);
    }

    private void removeCommentLikeRecord(Long userId, Long commentId) {
        userBehaviorService.remove(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getCommentId, commentId)
                .eq(UserBehavior::getActionType, "like"));
    }

    // ---- helpers ----

    private CommentVO buildCommentVO(Comment c, Map<Long, List<Comment>> parentMap, Map<Long, User> userMap, Long userId) {
        User author = userMap.get(c.getAuthorId());
        List<Comment> children = parentMap.getOrDefault(c.getCommentId(), Collections.emptyList());
        List<CommentVO> childVOs = children.stream()
                .map(child -> buildCommentVO(child, parentMap, userMap, userId))
                .collect(Collectors.toList());

        long likes = getRedisLong(RedisKeys.COMMENT_LIKES + c.getCommentId(), c.getLikes());
        boolean liked = userId != null && isLikedByUser(c.getCommentId(), userId);

        return CommentVO.builder()
                .commentId(c.getCommentId())
                .postId(c.getPostId())
                .authorId(c.getAuthorId())
                .authorName(author != null ? author.getUsername() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .parentCommentId(c.getParentCommentId())
                .content(c.getContent())
                .likes(likes)
                .liked(liked)
                .createTime(c.getCreateTime())
                .children(childVOs.isEmpty() ? null : childVOs)
                .build();
    }

    private Map<Long, User> loadUserMap(List<Comment> comments) {
        List<Long> userIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
    }

    private long getRedisLong(String key, long fallback) {
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            return val != null ? Long.parseLong(val) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public IPage<Comment> listPendingComments(int page, int pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getStatus, 1)
               .orderByDesc(Comment::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}

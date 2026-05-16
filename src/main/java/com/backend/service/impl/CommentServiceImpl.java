package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.dto.CommentCreateRequest;
import com.backend.dto.CommentVO;
import com.backend.entity.Comment;
import com.backend.entity.User;
import com.backend.mapper.CommentMapper;
import com.backend.service.CommentService;
import com.backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Long createComment(Long userId, CommentCreateRequest request) {
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setAuthorId(userId);
        comment.setParentCommentId(request.getParentCommentId());
        comment.setContent(request.getContent());
        comment.setLikes(0L);
        comment.setStatus(1); // 审核中
        save(comment);
        return comment.getCommentId();
    }

    @Override
    public List<CommentVO> getCommentTree(Long postId) {
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
                .map(root -> buildCommentVO(root, parentMap, userMap))
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
        String likeKey = "comment:likes:" + commentId;
        String likedSetKey = "comment:liked:" + commentId;

        if ("like".equals(action)) {
            Long added = stringRedisTemplate.opsForSet().add(likedSetKey, userId.toString());
            if (added != null && added > 0) {
                return stringRedisTemplate.opsForValue().increment(likeKey);
            }
        } else if ("unlike".equals(action)) {
            Long removed = stringRedisTemplate.opsForSet().remove(likedSetKey, userId.toString());
            if (removed != null && removed > 0) {
                return stringRedisTemplate.opsForValue().decrement(likeKey);
            }
        }
        String count = stringRedisTemplate.opsForValue().get(likeKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    // ---- helpers ----

    private CommentVO buildCommentVO(Comment c, Map<Long, List<Comment>> parentMap, Map<Long, User> userMap) {
        User author = userMap.get(c.getAuthorId());
        List<Comment> children = parentMap.getOrDefault(c.getCommentId(), Collections.emptyList());
        List<CommentVO> childVOs = children.stream()
                .map(child -> buildCommentVO(child, parentMap, userMap))
                .collect(Collectors.toList());

        return CommentVO.builder()
                .commentId(c.getCommentId())
                .postId(c.getPostId())
                .authorId(c.getAuthorId())
                .authorName(author != null ? author.getUsername() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .parentCommentId(c.getParentCommentId())
                .content(c.getContent())
                .likes(c.getLikes())
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
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
    }
}

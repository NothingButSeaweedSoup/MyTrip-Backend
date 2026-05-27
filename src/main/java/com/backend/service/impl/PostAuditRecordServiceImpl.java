package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.entity.Comment;
import com.backend.entity.Post;
import com.backend.entity.PostAuditRecord;
import com.backend.entity.User;
import com.backend.mapper.PostAuditRecordMapper;
import com.backend.service.CommentService;
import com.backend.service.PostAuditRecordService;
import com.backend.service.PostService;
import com.backend.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostAuditRecordServiceImpl
        extends ServiceImpl<PostAuditRecordMapper, PostAuditRecord>
        implements PostAuditRecordService {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void auditPost(Long auditorId, Long postId, String action, String remark) {
        checkAuditRole(auditorId);

        Post post = postService.getById(postId);
        if (post == null || post.getStatus() == 4) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != 0) {
            throw new BusinessException("帖子状态不是待审核");
        }

        int newStatus = "approve".equals(action) ? 1 : 2;

        // 更新帖子状态
        post.setStatus(newStatus);
        postService.updateById(post);

        // 记录审核日志
        PostAuditRecord record = new PostAuditRecord();
        record.setPostId(postId);
        record.setAuditorId(auditorId);
        record.setAuditType(1); // 人工
        record.setAction("approve".equals(action) ? 1 : 2);
        record.setReason(remark);
        record.setOldStatus(0);
        record.setNewStatus(newStatus);
        save(record);
    }

    @Override
    @Transactional
    public void autoAuditPost(Long postId, int action, String reason) {
        Post post = postService.getById(postId);
        if (post == null) return;

        int newStatus;
        switch (action) {
            case 1 -> newStatus = 1;  // 通过
            case 2 -> newStatus = 2;  // 驳回
            default -> newStatus = 0; // 转人工，状态不变
        }

        if (newStatus != 0) {
            post.setStatus(newStatus);
            postService.updateById(post);
        }

        PostAuditRecord record = new PostAuditRecord();
        record.setPostId(postId);
        record.setAuditorId(null);
        record.setAuditType(0); // 自动
        record.setAction(action);
        record.setReason(reason);
        record.setOldStatus(0);
        record.setNewStatus(newStatus);
        save(record);
    }

    @Override
    @Transactional
    public void auditComment(Long auditorId, Long commentId, String action, String remark) {
        checkAuditRole(auditorId);

        Comment comment = commentService.getById(commentId);
        if (comment == null || comment.getStatus() == 2) {
            throw new BusinessException("评论不存在");
        }
        if (comment.getStatus() != 1) {
            throw new BusinessException("评论状态不是审核中");
        }

        int newStatus = "approve".equals(action) ? 0 : 2;

        comment.setStatus(newStatus);
        commentService.updateById(comment);

        // 记录审核日志（复用同一张表，post_id 存帖子的ID，不做严格要求）
        // 评论审核目前不记录到 post_audit_record，简化处理
    }

    private void checkAuditRole(Long userId) {
        User user = userService.getById(userId);
        if (user == null || (user.getRole() != 1 && user.getRole() != 9)) {
            throw new UnauthorizedException("无审核权限");
        }
    }
}

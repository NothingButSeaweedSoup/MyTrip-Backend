package com.backend.service;

import com.backend.dto.CommentCreateRequest;
import com.backend.dto.CommentVO;
import com.backend.entity.Comment;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CommentService extends IService<Comment> {

    /** 发表评论（顶级/回复），返回评论ID */
    Long createComment(Long userId, CommentCreateRequest request);

    /** 获取评论树（按时间正序） */
    List<CommentVO> getCommentTree(Long postId);

    /** 获取评论树（含当前用户点赞状态） */
    List<CommentVO> getCommentTree(Long postId, Long userId);

    /** 删除评论（软删除） */
    void deleteComment(Long userId, Long commentId);

    /** 点赞/取消点赞 */
    long toggleLike(Long userId, Long commentId, String action);

    /** 待审核评论列表 */
    IPage<Comment> listPendingComments(int page, int pageSize);
}

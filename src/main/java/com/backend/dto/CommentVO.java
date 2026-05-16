package com.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class CommentVO {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private Long parentCommentId;
    private String content;
    private Long likes;
    private Date createTime;

    /** 子回复列表（树形结构） */
    private List<CommentVO> children;
}

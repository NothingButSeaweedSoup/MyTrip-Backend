package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /** 父评论ID，为null则为顶级评论 */
    private Long parentCommentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}

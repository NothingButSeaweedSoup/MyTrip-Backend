package com.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class PostVO {
    private Long postId;
    private String hashId;
    private String title;
    private String content;
    private Long views;
    private Long likes;
    private Integer commentCount;
    private BigDecimal hotScore;
    private Date createTime;

    /** 作者信息 */
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 当前用户是否已收藏 */
    private Boolean favorited;

    /** 图片URL列表 */
    private List<String> images;

    /** 标签列表 */
    private List<TagInfo> tags;

    @Data
    @Builder
    public static class TagInfo {
        private Integer tagId;
        private String name;
    }
}

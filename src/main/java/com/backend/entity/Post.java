package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 帖子表
 * @TableName post
 */
@TableName(value ="post")
@Data
public class Post {
    /**
     * 帖子ID
     */
    @TableId(type = IdType.AUTO)
    private Long postId;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 哈希ID
     */
    private String hashId;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 浏览量 (Redis异步写回)
     */
    private Long views;

    /**
     * 点赞量 (Redis异步写回)
     */
    private Long likes;

    /**
     * 评论总数 (冗余字段)
     */
    private Integer commentCount;

    /**
     * 状态: 0-待审核, 1-通过, 2-驳回, 3-仅自己可见, 4-已删除
     */
    private Integer status;

    /**
     * 热度分 (定时任务更新)
     */
    private BigDecimal hotScore;

    /**
     * 热度分更新时间
     */
    private Date scoreUpdatedAt;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Post other = (Post) that;
        return (this.getPostId() == null ? other.getPostId() == null : this.getPostId().equals(other.getPostId()))
            && (this.getAuthorId() == null ? other.getAuthorId() == null : this.getAuthorId().equals(other.getAuthorId()))
            && (this.getHashId() == null ? other.getHashId() == null : this.getHashId().equals(other.getHashId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getViews() == null ? other.getViews() == null : this.getViews().equals(other.getViews()))
            && (this.getLikes() == null ? other.getLikes() == null : this.getLikes().equals(other.getLikes()))
            && (this.getCommentCount() == null ? other.getCommentCount() == null : this.getCommentCount().equals(other.getCommentCount()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getHotScore() == null ? other.getHotScore() == null : this.getHotScore().equals(other.getHotScore()))
            && (this.getScoreUpdatedAt() == null ? other.getScoreUpdatedAt() == null : this.getScoreUpdatedAt().equals(other.getScoreUpdatedAt()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPostId() == null) ? 0 : getPostId().hashCode());
        result = prime * result + ((getAuthorId() == null) ? 0 : getAuthorId().hashCode());
        result = prime * result + ((getHashId() == null) ? 0 : getHashId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getViews() == null) ? 0 : getViews().hashCode());
        result = prime * result + ((getLikes() == null) ? 0 : getLikes().hashCode());
        result = prime * result + ((getCommentCount() == null) ? 0 : getCommentCount().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getHotScore() == null) ? 0 : getHotScore().hashCode());
        result = prime * result + ((getScoreUpdatedAt() == null) ? 0 : getScoreUpdatedAt().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", postId=").append(postId);
        sb.append(", authorId=").append(authorId);
        sb.append(", hashId=").append(hashId);
        sb.append(", title=").append(title);
        sb.append(", content=").append(content);
        sb.append(", views=").append(views);
        sb.append(", likes=").append(likes);
        sb.append(", commentCount=").append(commentCount);
        sb.append(", status=").append(status);
        sb.append(", hotScore=").append(hotScore);
        sb.append(", scoreUpdatedAt=").append(scoreUpdatedAt);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}
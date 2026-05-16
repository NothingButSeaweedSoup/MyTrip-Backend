package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户行为表
 * @TableName user_behavior
 */
@TableName(value ="user_behavior")
@Data
public class UserBehavior {
    /**
     * 行为ID
     */
    @TableId(type = IdType.AUTO)
    private Long behaviorId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 行为类型
     */
    private Object actionType;

    /**
     * 浏览时长(秒) (仅view有效)
     */
    private Integer duration;

    /**
     * 发生时间
     */
    private Date createTime;

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
        UserBehavior other = (UserBehavior) that;
        return (this.getBehaviorId() == null ? other.getBehaviorId() == null : this.getBehaviorId().equals(other.getBehaviorId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getPostId() == null ? other.getPostId() == null : this.getPostId().equals(other.getPostId()))
            && (this.getActionType() == null ? other.getActionType() == null : this.getActionType().equals(other.getActionType()))
            && (this.getDuration() == null ? other.getDuration() == null : this.getDuration().equals(other.getDuration()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getBehaviorId() == null) ? 0 : getBehaviorId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getPostId() == null) ? 0 : getPostId().hashCode());
        result = prime * result + ((getActionType() == null) ? 0 : getActionType().hashCode());
        result = prime * result + ((getDuration() == null) ? 0 : getDuration().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", behaviorId=").append(behaviorId);
        sb.append(", userId=").append(userId);
        sb.append(", postId=").append(postId);
        sb.append(", actionType=").append(actionType);
        sb.append(", duration=").append(duration);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}
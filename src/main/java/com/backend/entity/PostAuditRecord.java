package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 统一审核记录表
 * @TableName post_audit_record
 */
@TableName(value ="post_audit_record")
@Data
public class PostAuditRecord {
    /**
     * 审核记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long auditId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 审核者ID (自动审核为NULL或0)
     */
    private Long auditorId;

    /**
     * 审核类型: 0-自动, 1-人工
     */
    private Integer auditType;

    /**
     * 审核动作: 1-通过, 2-驳回, 3-转人工
     */
    private Integer action;

    /**
     * 原因/意见
     */
    private String reason;

    /**
     * 审核前状态
     */
    private Integer oldStatus;

    /**
     * 审核后状态
     */
    private Integer newStatus;

    /**
     * 审核时间
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
        PostAuditRecord other = (PostAuditRecord) that;
        return (this.getAuditId() == null ? other.getAuditId() == null : this.getAuditId().equals(other.getAuditId()))
            && (this.getPostId() == null ? other.getPostId() == null : this.getPostId().equals(other.getPostId()))
            && (this.getAuditorId() == null ? other.getAuditorId() == null : this.getAuditorId().equals(other.getAuditorId()))
            && (this.getAuditType() == null ? other.getAuditType() == null : this.getAuditType().equals(other.getAuditType()))
            && (this.getAction() == null ? other.getAction() == null : this.getAction().equals(other.getAction()))
            && (this.getReason() == null ? other.getReason() == null : this.getReason().equals(other.getReason()))
            && (this.getOldStatus() == null ? other.getOldStatus() == null : this.getOldStatus().equals(other.getOldStatus()))
            && (this.getNewStatus() == null ? other.getNewStatus() == null : this.getNewStatus().equals(other.getNewStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getAuditId() == null) ? 0 : getAuditId().hashCode());
        result = prime * result + ((getPostId() == null) ? 0 : getPostId().hashCode());
        result = prime * result + ((getAuditorId() == null) ? 0 : getAuditorId().hashCode());
        result = prime * result + ((getAuditType() == null) ? 0 : getAuditType().hashCode());
        result = prime * result + ((getAction() == null) ? 0 : getAction().hashCode());
        result = prime * result + ((getReason() == null) ? 0 : getReason().hashCode());
        result = prime * result + ((getOldStatus() == null) ? 0 : getOldStatus().hashCode());
        result = prime * result + ((getNewStatus() == null) ? 0 : getNewStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", auditId=").append(auditId);
        sb.append(", postId=").append(postId);
        sb.append(", auditorId=").append(auditorId);
        sb.append(", auditType=").append(auditType);
        sb.append(", action=").append(action);
        sb.append(", reason=").append(reason);
        sb.append(", oldStatus=").append(oldStatus);
        sb.append(", newStatus=").append(newStatus);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}
package com.backend.service;

import com.backend.dto.AuditRecordVO;
import com.backend.entity.PostAuditRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostAuditRecordService extends IService<PostAuditRecord> {

    /** 审核帖子（通过/驳回），记录审核日志 */
    void auditPost(Long auditorId, Long postId, String action, String remark);

    /** 自动审核帖子，记录审核日志（auditType=0） */
    void autoAuditPost(Long postId, int action, String reason);

    /** 审核评论（通过/驳回），记录审核日志 */
    void auditComment(Long auditorId, Long commentId, String action, String remark);

    /** 管理员：获取审核历史 */
    IPage<AuditRecordVO> getAuditHistory(Long adminUserId, int page, int pageSize, String targetType);
}

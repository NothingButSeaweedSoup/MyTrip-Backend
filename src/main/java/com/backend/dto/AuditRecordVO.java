package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AuditRecordVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String decision;
    private String reason;
    private String auditorName;
    private Date createTime;
}

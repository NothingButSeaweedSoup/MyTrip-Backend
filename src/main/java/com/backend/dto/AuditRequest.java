package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuditRequest {

    @NotBlank
    @Pattern(regexp = "approve|reject", message = "action 仅限 approve / reject")
    private String action;

    /** 审核意见 */
    private String remark;
}

package com.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class UserVO {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String bio;
    private List<Integer> preferredTags;
    private String budgetLevel;
    private Integer role;
    private Date createTime;
}

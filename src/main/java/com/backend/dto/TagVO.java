package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class TagVO {
    private Integer tagId;
    private String name;
    private Integer useCount;
    private Date createTime;
}

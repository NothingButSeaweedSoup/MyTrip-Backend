package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class PostCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长128字")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String content;

    /** 图片URL列表（先上传图片获取URL后再提交帖子） */
    private List<String> images;

    private List<Integer> tagIds;
}

package com.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class PostEditRequest {

    @Size(max = 128, message = "标题最长128字")
    private String title;

    private String content;

    /** 新增的图片URL列表 */
    private List<String> imagesToAdd;

    /** 要删除的图片ID列表 */
    private List<Long> imageIdsToDelete;

    /** 替换标签（传空列表则清空标签） */
    private List<Integer> tagIds;
}

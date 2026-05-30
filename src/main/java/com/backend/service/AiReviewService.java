package com.backend.service;

import com.backend.dto.AiPromptVO;

import java.util.List;

public interface AiReviewService {

    /**
     * AI 审核单条帖子（纯文本）
     * @param title   帖子标题
     * @param content 帖子内容
     * @return 审核结果
     */
    AiReviewResult review(String title, String content);

    /**
     * AI 审核帖子（含图片）
     * @param title     帖子标题
     * @param content   帖子内容
     * @param imageUrls 图片完整URL列表
     * @return 审核结果
     */
    AiReviewResult reviewWithImages(String title, String content, List<String> imageUrls);

    /** 获取文本审核提示词 */
    AiPromptVO getPromptConfig();

    /** 更新文本审核提示词 */
    void updatePrompt(String prompt);

    /** 获取图像审核提示词 */
    AiPromptVO getImagePromptConfig();

    /** 更新图像审核提示词 */
    void updateImagePrompt(String prompt);

    record AiReviewResult(String decision, String reason) {
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String NEED_MANUAL = "NEED_MANUAL";

        public boolean isApproved() {
            return APPROVED.equals(decision);
        }

        public boolean isRejected() {
            return REJECTED.equals(decision);
        }

        public boolean needsManual() {
            return NEED_MANUAL.equals(decision);
        }
    }
}

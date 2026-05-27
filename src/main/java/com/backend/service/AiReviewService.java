package com.backend.service;

public interface AiReviewService {

    /**
     * AI 审核单条帖子
     * @param title   帖子标题
     * @param content 帖子内容
     * @return 审核结果
     */
    AiReviewResult review(String title, String content);

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

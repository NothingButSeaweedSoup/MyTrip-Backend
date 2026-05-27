package com.backend.service.impl;

import com.backend.service.AiReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiReviewServiceImpl implements AiReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一个内容审核助手。请审核以下帖子内容，判断是否违规。

            审核规则：
            1. 禁止发布违法信息（赌博、毒品、枪支等）
            2. 禁止发布色情低俗内容
            3. 禁止发布广告、垃圾营销信息
            4. 禁止发布人身攻击、辱骂内容
            5. 禁止泄露他人隐私

            请返回以下 JSON 格式的审核结果（不要包含其他内容）：
            {"decision": "APPROVED", "reason": "审核理由"}

            decision 必须是以下之一：
            - APPROVED：内容合规，通过
            - REJECTED：明显违规，驳回（reason 说明违规原因）
            - NEED_MANUAL：无法确定，转人工审核
            """;

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\{\\s*\"decision\"\\s*:\\s*\"(APPROVED|REJECTED|NEED_MANUAL)\"\\s*,\\s*\"reason\"\\s*:\\s*\"([^\"]*)\"\\s*}",
            Pattern.DOTALL);

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AiReviewResult review(String title, String content) {
        try {
            String prompt = buildPrompt(title, content);
            String response = chatModel.chat(prompt);

            AiReviewResult result = parseResult(response);
            log.info("AI 审核完成: decision={}, reason={}", result.decision(), result.reason());
            return result;
        } catch (Exception e) {
            log.error("AI 审核异常，降级为 NEED_MANUAL: {}", e.getMessage());
            return new AiReviewResult(AiReviewResult.NEED_MANUAL, "AI 审核服务异常，转人工");
        }
    }

    private String buildPrompt(String title, String content) {
        return SYSTEM_PROMPT + "\n\n帖子标题：" + title + "\n帖子内容：" + content;
    }

    /**
     * 解析 AI 返回的 JSON 结果
     * 优先正则提取，失败则尝试完整 JSON 解析
     */
    AiReviewResult parseResult(String json) {
        if (json == null || json.isBlank()) {
            return new AiReviewResult(AiReviewResult.NEED_MANUAL, "AI 返回为空");
        }

        // 尝试正则提取
        Matcher matcher = JSON_PATTERN.matcher(json);
        if (matcher.find()) {
            return new AiReviewResult(matcher.group(1), matcher.group(2));
        }

        // 尝试完整 JSON 解析
        try {
            JsonNode node = objectMapper.readTree(json);
            String decision = node.has("decision") ? node.get("decision").asText() : null;
            String reason = node.has("reason") ? node.get("reason").asText() : "";
            if (decision != null) {
                return new AiReviewResult(decision, reason);
            }
        } catch (Exception ignored) {
        }

        log.warn("AI 审核结果解析失败: {}", json);
        return new AiReviewResult(AiReviewResult.NEED_MANUAL, "AI 审核结果解析失败");
    }
}

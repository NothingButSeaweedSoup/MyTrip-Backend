package com.backend.service.impl;

import com.backend.common.RedisKeys;
import com.backend.dto.AiPromptVO;
import com.backend.service.AiReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiReviewServiceImpl implements AiReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewServiceImpl.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
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

    private static final String IMAGE_REVIEW_SYSTEM_PROMPT = """
            你是一个内容审核助手。请审核以下帖子的文本内容和图片，判断是否违规。

            审核规则：
            1. 禁止发布违法信息（赌博、毒品、枪支等）
            2. 禁止发布色情低俗、暴露、性暗示内容
            3. 禁止发布广告、垃圾营销信息
            4. 禁止发布人身攻击、辱骂内容
            5. 禁止泄露他人隐私
            6. 禁止发布血腥暴力、恐怖恶心的图片
            7. 禁止发布含有二维码、联系方式等引流图片
            8. 禁止发布违反法律法规的图片内容

            请综合文本和图片内容进行审核，返回以下 JSON 格式的结果（不要包含其他内容）：
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

    @Autowired(required = false)
    @Qualifier("visionChatModel")
    private ChatModel visionChatModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${ai.vision.max-images:9}")
    private int maxImages;

    private volatile String cachedPrompt;
    private volatile long cacheTime;
    private static final long CACHE_TTL = 60_000;

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

    @Override
    public AiReviewResult reviewWithImages(String title, String content, List<String> imageUrls) {
        if (visionChatModel == null) {
            log.warn("视觉模型未配置，降级为纯文本审核");
            return review(title, content);
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            return review(title, content);
        }

        try {
            List<String> urls = imageUrls.size() > maxImages
                    ? imageUrls.subList(0, maxImages) : imageUrls;

            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(IMAGE_REVIEW_SYSTEM_PROMPT + "\n\n帖子标题：" + title + "\n帖子内容：" + content));
            for (String url : urls) {
                contents.add(ImageContent.from(url));
            }

            UserMessage userMessage = UserMessage.from(contents);
            ChatRequest request = ChatRequest.builder()
                    .messages(userMessage)
                    .build();
            ChatResponse response = visionChatModel.chat(request);
            String text = response.aiMessage().text();

            AiReviewResult result = parseResult(text);
            log.info("AI 图片审核完成: decision={}, reason={}, images={}", result.decision(), result.reason(), urls.size());
            return result;
        } catch (Exception e) {
            log.error("AI 图片审核异常，降级为纯文本审核: {}", e.getMessage());
            return review(title, content);
        }
    }

    @Override
    public AiPromptVO getPromptConfig() {
        String currentPrompt = getSystemPrompt();
        String lastUpdate = redisTemplate.opsForValue().get(RedisKeys.AI_REVIEW_PROMPT_UPDATE_TIME);
        Date updateTime = lastUpdate != null ? new Date(Long.parseLong(lastUpdate)) : null;
        return AiPromptVO.builder()
                .prompt(currentPrompt)
                .defaultPrompt(DEFAULT_SYSTEM_PROMPT)
                .lastUpdateTime(updateTime)
                .build();
    }

    @Override
    public void updatePrompt(String prompt) {
        redisTemplate.opsForValue().set(RedisKeys.AI_REVIEW_PROMPT, prompt);
        redisTemplate.opsForValue().set(RedisKeys.AI_REVIEW_PROMPT_UPDATE_TIME,
                String.valueOf(System.currentTimeMillis()), 365, TimeUnit.DAYS);
        cachedPrompt = prompt;
        cacheTime = System.currentTimeMillis();
        log.info("AI 审核提示词已更新");
    }

    private String getSystemPrompt() {
        if (cachedPrompt != null && System.currentTimeMillis() - cacheTime < CACHE_TTL) {
            return cachedPrompt;
        }
        String prompt = redisTemplate.opsForValue().get(RedisKeys.AI_REVIEW_PROMPT);
        if (prompt != null && !prompt.isBlank()) {
            cachedPrompt = prompt;
            cacheTime = System.currentTimeMillis();
            return prompt;
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    private String buildPrompt(String title, String content) {
        return getSystemPrompt() + "\n\n帖子标题：" + title + "\n帖子内容：" + content;
    }

    AiReviewResult parseResult(String json) {
        if (json == null || json.isBlank()) {
            return new AiReviewResult(AiReviewResult.NEED_MANUAL, "AI 返回为空");
        }

        Matcher matcher = JSON_PATTERN.matcher(json);
        if (matcher.find()) {
            return new AiReviewResult(matcher.group(1), matcher.group(2));
        }

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

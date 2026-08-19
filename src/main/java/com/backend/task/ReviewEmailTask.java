package com.backend.task;

import com.backend.common.RedisKeys;
import com.backend.entity.PostAuditRecord;
import com.backend.service.PostAuditRecordService;
import com.backend.service.ReviewNotificationEmailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

@Component
public class ReviewEmailTask {

    private static final Logger log = LoggerFactory.getLogger(ReviewEmailTask.class);

    @Autowired
    private PostAuditRecordService auditRecordService;

    @Autowired
    private ReviewNotificationEmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private long lastCheckTime;

    /** 内部轮询间隔 30 秒，实际执行间隔由 Redis 配置决定 */
    @Scheduled(fixedDelay = 30_000)
    public void checkAndNotify() {
        try {
            checkAndNotifyInternal();
        } catch (Exception e) {
            log.warn("checkAndNotify failed: {}", e.getMessage());
        }
    }

    private void checkAndNotifyInternal() {
        String enabledStr = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_ENABLED);
        if (!"true".equals(enabledStr)) {
            return;
        }

        int intervalMin = getIntervalMinutes();
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < intervalMin * 60_000L) {
            return;
        }
        lastCheckTime = now;

        List<String> recipients = emailService.listEnabledEmails();
        if (recipients.isEmpty()) {
            return;
        }

        String host = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SMTP_HOST);
        if (host == null || host.isBlank()) {
            log.debug("SMTP 未配置，跳过邮件通知");
            return;
        }

        String lastIdStr = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_LAST_AUDIT_ID);
        long lastAuditId = lastIdStr != null ? Long.parseLong(lastIdStr) : 0L;

        LambdaQueryWrapper<PostAuditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAuditRecord::getAction, 3)
               .gt(PostAuditRecord::getAuditId, lastAuditId)
               .orderByDesc(PostAuditRecord::getAuditId);

        List<PostAuditRecord> newPending = auditRecordService.list(wrapper);

        if (newPending.isEmpty()) {
            return;
        }

        long maxAuditId = newPending.get(0).getAuditId();
        String subject = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SUBJECT);
        if (subject == null || subject.isBlank()) {
            subject = "【MyTrip】新的待审核帖子通知";
        }

        String body = String.format(
                "您好，\n\n目前有 %d 条新的帖子需要人工审核，请登录后台处理。\n\n此邮件由系统自动发送，请勿回复。",
                newPending.size());

        try {
            JavaMailSenderImpl sender = createMailSender();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender.getUsername());
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);

            redisTemplate.opsForValue().set(RedisKeys.REVIEW_EMAIL_LAST_AUDIT_ID, String.valueOf(maxAuditId));
            log.info("审核通知邮件已发送: 收件人={}, 新增待审帖子={}, maxAuditId={}",
                    recipients.size(), newPending.size(), maxAuditId);
        } catch (Exception e) {
            log.error("审核通知邮件发送失败: {}", e.getMessage());
        }
    }

    private int getIntervalMinutes() {
        String val = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_INTERVAL_MINUTES);
        if (val != null && !val.isBlank()) {
            try { return Math.max(1, Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
        }
        return 5; // default 5 minutes
    }

    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SMTP_HOST));
        String port = redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SMTP_PORT);
        sender.setPort(Integer.parseInt(port != null && !port.isBlank() ? port : "587"));
        sender.setUsername(redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SMTP_USERNAME));
        sender.setPassword(redisTemplate.opsForValue().get(RedisKeys.REVIEW_EMAIL_SMTP_PASSWORD));
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        sender.setJavaMailProperties(props);
        return sender;
    }
}

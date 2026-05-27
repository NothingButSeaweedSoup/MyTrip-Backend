package com.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 加密敏感词库加载器
 * <p>
 * 读取 classpath:sensitive/words.enc，使用 AES-256-CBC 解密，
 * 返回敏感词列表（每行一个词）。
 */
@Component
public class SensitiveWordLoader {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordLoader.class);

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    @Value("${review.sensitive-word-key}")
    private String base64Key;

    /**
     * 从 classpath:sensitive/words.enc 读取并解密
     * @return 敏感词列表（已 trim，空行已过滤）
     */
    public List<String> loadSensitiveWords() {
        byte[] encryptedData = readResource();
        if (encryptedData == null) return List.of();

        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            log.error("sensitive-word-key 不是有效的 base64 编码", e);
            return List.of();
        }

        if (key.length != 32) {
            log.error("sensitive-word-key 解密后长度必须为 32 字节，当前为 {} 字节", key.length);
            return List.of();
        }

        byte[] decrypted = decryptAes(encryptedData, key);
        if (decrypted == null) return List.of();

        String text = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        List<String> words = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                words.add(trimmed);
            }
        }

        log.info("敏感词库加载完成: {} 个词", words.size());
        return words;
    }

    private byte[] readResource() {
        try {
            ClassPathResource resource = new ClassPathResource("sensitive/words.enc");
            if (!resource.exists()) {
                log.warn("敏感词库文件不存在: classpath:sensitive/words.enc");
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            log.error("读取敏感词库文件失败", e);
            return null;
        }
    }

    /**
     * AES-256-CBC 解密
     * @param encrypted IV(16字节) + 密文
     * @param key       32字节 AES 密钥
     * @return 解密后的明文
     */
    private byte[] decryptAes(byte[] encrypted, byte[] key) {
        if (encrypted.length < IV_LENGTH) {
            log.error("加密数据不足 {} 字节", IV_LENGTH);
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[encrypted.length - IV_LENGTH];
            System.arraycopy(encrypted, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            return null;
        }
    }
}

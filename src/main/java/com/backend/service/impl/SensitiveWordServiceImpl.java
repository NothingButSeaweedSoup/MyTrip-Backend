package com.backend.service.impl;

import com.backend.dto.SensitiveWordListVO;
import com.backend.service.SensitiveWordService;
import com.backend.util.ACAutomaton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordServiceImpl.class);
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final String CUSTOM_FILE = "sensitive/words_custom.enc";

    @Value("${review.sensitive-word-key}")
    private String base64Key;

    @Autowired
    private ACAutomaton acAutomaton;

    @Override
    public SensitiveWordListVO listWords() {
        List<String> baseWords = decryptFromBaseFile();
        List<String> customWords = decryptFromCustomFile();
        return new SensitiveWordListVO(baseWords.size(), customWords.size(), customWords);
    }

    @Override
    public void addWord(String word) {
        String trimmed = word.trim();
        if (trimmed.isEmpty()) return;

        List<String> customWords = decryptFromCustomFile();
        if (customWords.contains(trimmed)) return;

        customWords.add(trimmed);
        encryptToCustomFile(customWords);
        rebuildAutomaton();
        log.info("自定义敏感词已添加: {}", trimmed);
    }

    @Override
    public void removeWord(String word) {
        List<String> customWords = decryptFromCustomFile();
        if (!customWords.remove(word)) return;

        encryptToCustomFile(customWords);
        rebuildAutomaton();
        log.info("自定义敏感词已移除: {}", word);
    }

    @Override
    public void reload() {
        rebuildAutomaton();
        log.info("敏感词已重新加载");
    }

    private void rebuildAutomaton() {
        List<String> allWords = new ArrayList<>();
        allWords.addAll(decryptFromBaseFile());
        allWords.addAll(decryptFromCustomFile());
        acAutomaton.build(allWords);
        log.info("AC 自动机已重建: 基础 {} + 自定义 {} = {} 个词",
                decryptFromBaseFile().size(), decryptFromCustomFile().size(), allWords.size());
    }

    // ========== 基础词库 (words.enc，只读) ==========

    private List<String> decryptFromBaseFile() {
        byte[] data = readClassPathResource("sensitive/words.enc");
        return decryptData(data);
    }

    // ========== 自定义词库 (words_custom.enc，可读写) ==========

    private List<String> decryptFromCustomFile() {
        byte[] data = readClassPathResource(CUSTOM_FILE);
        return decryptData(data);
    }

    private void encryptToCustomFile(List<String> words) {
        byte[] key = decodeKey();
        if (key == null) return;

        String text = String.join("\n", words);
        byte[] plaintext = text.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] output = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, output, IV_LENGTH, ciphertext.length);

            Path filePath = resolveCustomFilePath();
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, output);
            log.info("自定义敏感词文件已更新: {} 个词 -> {}", words.size(), filePath);
        } catch (Exception e) {
            log.error("加密写入自定义敏感词文件失败", e);
            throw new RuntimeException("保存敏感词文件失败", e);
        }
    }

    private Path resolveCustomFilePath() {
        try {
            ClassPathResource resource = new ClassPathResource(CUSTOM_FILE);
            File file = resource.getFile();
            if (!file.exists()) {
                // Create empty encrypted file if it doesn't exist
                file.getParentFile().mkdirs();
                encryptToCustomFile(List.of());
            }
            return file.toPath();
        } catch (IOException e) {
            throw new RuntimeException("无法解析自定义敏感词文件路径", e);
        }
    }

    // ========== 通用加解密 ==========

    private byte[] readClassPathResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            log.error("读取资源文件失败: {}", path, e);
            return null;
        }
    }

    private List<String> decryptData(byte[] encryptedData) {
        if (encryptedData == null || encryptedData.length < IV_LENGTH) return new ArrayList<>();

        byte[] key = decodeKey();
        if (key == null) return new ArrayList<>();

        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[encryptedData.length - IV_LENGTH];
            System.arraycopy(encryptedData, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(ciphertext);

            String text = new String(decrypted, StandardCharsets.UTF_8);
            List<String> words = new ArrayList<>();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) words.add(trimmed);
            }
            return words;
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            return new ArrayList<>();
        }
    }

    private byte[] decodeKey() {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            if (key.length != 32) {
                log.error("密钥长度必须为32字节，当前为{}字节", key.length);
                return null;
            }
            return key;
        } catch (IllegalArgumentException e) {
            log.error("sensitive-word-key 不是有效的 base64 编码", e);
            return null;
        }
    }
}

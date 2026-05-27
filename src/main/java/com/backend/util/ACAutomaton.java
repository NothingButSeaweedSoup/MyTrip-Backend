package com.backend.util;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Aho-Corasick 自动机 — 多模式字符串匹配
 * <p>
 * 构建 Trie 树 + fail 指针，实现 O(n) 级别的敏感词匹配。
 * 支持中文等多字节字符（Unicode codepoint）。
 */
@Component
public class ACAutomaton {

    private static final Logger log = LoggerFactory.getLogger(ACAutomaton.class);

    private volatile TrieNode root;

    @Autowired
    private SensitiveWordLoader sensitiveWordLoader;

    /** 启动时自动加载敏感词并构建 AC 自动机 */
    @PostConstruct
    public void init() {
        List<String> words = sensitiveWordLoader.loadSensitiveWords();
        build(words);
    }

    /** 构建 Trie 树 + fail 指针 */
    public void build(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            root = new TrieNode();
            log.warn("ACAutomaton 使用空词库构建");
            return;
        }

        TrieNode newRoot = new TrieNode();

        // 1. 构建 Trie 树
        for (String keyword : keywords) {
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) continue;

            TrieNode cur = newRoot;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
            }
            cur.keyword = trimmed;
        }

        // 2. BFS 构建 fail 指针
        Queue<TrieNode> queue = new LinkedList<>();
        for (TrieNode child : newRoot.children.values()) {
            child.fail = newRoot;
            queue.offer(child);
        }

        while (!queue.isEmpty()) {
            TrieNode cur = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : cur.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();

                // 沿父节点的 fail 指针查找匹配
                TrieNode f = cur.fail;
                while (f != null && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                child.fail = (f != null) ? f.children.get(c) : newRoot;

                // 合并 fail 节点的 keyword（避免 matchAll 遗漏）
                if (child.fail != null && child.fail.keyword != null) {
                    child.keyword = child.fail.keyword;  // 简化：仅记录最近一个
                }

                queue.offer(child);
            }
        }

        root = newRoot;
        log.info("ACAutomaton 构建完成: {} 个敏感词", keywords.size());
    }

    /**
     * 检测文本是否包含敏感词，返回第一个命中的敏感词
     * @return 命中的敏感词，无命中返回 null
     */
    public String match(String text) {
        if (root == null || text == null || text.isEmpty()) return null;

        TrieNode cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }
            cur = cur.children.getOrDefault(c, root);

            // 检查当前节点及其 fail 链是否命中
            TrieNode check = cur;
            while (check != root) {
                if (check.keyword != null) {
                    return check.keyword;
                }
                check = check.fail;
            }
        }

        return null;
    }

    /**
     * 检测文本是否包含敏感词，返回所有命中的敏感词（去重）
     */
    public List<String> matchAll(String text) {
        if (root == null || text == null || text.isEmpty()) return Collections.emptyList();

        Set<String> hits = new LinkedHashSet<>();
        TrieNode cur = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }
            cur = cur.children.getOrDefault(c, root);

            TrieNode check = cur;
            while (check != root) {
                if (check.keyword != null) {
                    hits.add(check.keyword);
                }
                check = check.fail;
            }
        }

        return new ArrayList<>(hits);
    }

    static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        TrieNode fail;
        String keyword; // 非 null 表示该节点是某个敏感词的结尾
    }
}

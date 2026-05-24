package com.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ImageUrlUtil {

    @Value("${base-img-url:}")
    private String baseImgUrl;

    /**
     * 将数据库中相对路径拼接为完整URL
     */
    public String getFullUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return relativePath;
        }
        if (!StringUtils.hasText(baseImgUrl)) {
            return relativePath;
        }
        // 去掉重复的斜杠
        String base = baseImgUrl.endsWith("/") ? baseImgUrl.substring(0, baseImgUrl.length() - 1) : baseImgUrl;
        String path = relativePath.replace('\\', '/');
        path = path.startsWith("/") ? path : "/" + path;
        return base + path;
    }
}

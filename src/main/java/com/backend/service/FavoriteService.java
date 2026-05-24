package com.backend.service;

import com.backend.entity.Favorite;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface FavoriteService extends IService<Favorite> {

    /** 收藏/取消收藏，返回最新收藏数 */
    long toggleFavorite(Long userId, Long postId, String action);

    /** 检查用户是否收藏了某帖子 */
    boolean isFavorited(Long userId, Long postId);

    /** 获取帖子收藏数 */
    long getFavoriteCount(Long postId);

    /** 获取用户收藏的帖子ID列表（分页） */
    List<Long> getUserFavoriteIds(Long userId, int page, int pageSize);
}

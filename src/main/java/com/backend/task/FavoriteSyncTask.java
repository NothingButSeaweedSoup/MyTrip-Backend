package com.backend.task;

import com.backend.common.RedisKeys;
import com.backend.entity.Favorite;
import com.backend.service.FavoriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FavoriteSyncTask {

    private static final Logger log = LoggerFactory.getLogger(FavoriteSyncTask.class);

    private static final long MIN_INTERVAL_MS = 30_000;
    private static final int FLUSH_THRESHOLD = 50;

    private long lastSyncAt;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FavoriteService favoriteService;

    @Scheduled(fixedDelay = 5_000)
    public void syncFavorites() {
        try {
            Long size = stringRedisTemplate.opsForSet().size(RedisKeys.DIRTY_FAVORITES);
            if (size == null || size == 0) return;

            long now = System.currentTimeMillis();
            if (size < FLUSH_THRESHOLD && now - lastSyncAt < MIN_INTERVAL_MS) {
                return;
            }

            Set<String> dirtyIds = stringRedisTemplate.opsForSet().members(RedisKeys.DIRTY_FAVORITES);
            if (dirtyIds == null || dirtyIds.isEmpty()) return;

            for (String idStr : dirtyIds) {
                Long postId = Long.parseLong(idStr);

                Set<Long> redisUserIds = getRedisFavoriteUsers(postId);
                Set<Long> dbUserIds = getDbFavoriteUsers(postId);

                Set<Long> toInsert = new HashSet<>(redisUserIds);
                toInsert.removeAll(dbUserIds);

                Set<Long> toDelete = new HashSet<>(dbUserIds);
                toDelete.removeAll(redisUserIds);

                for (Long userId : toInsert) {
                    Favorite fav = new Favorite();
                    fav.setUserId(userId);
                    fav.setPostId(postId);
                    favoriteService.save(fav);
                }

                if (!toDelete.isEmpty()) {
                    favoriteService.remove(new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getPostId, postId)
                            .in(Favorite::getUserId, toDelete));
                }

                stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_FAVORITES, idStr);
            }
            lastSyncAt = now;
            log.info("FavoriteSync completed, {} posts reconciled", dirtyIds.size());
        } catch (Exception e) {
            log.warn("syncFavorites failed: {}", e.getMessage());
        }
    }

    private Set<Long> getRedisFavoriteUsers(Long postId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(RedisKeys.FAVORITE_SET + postId);
        if (members == null || members.isEmpty()) return Set.of();
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    private Set<Long> getDbFavoriteUsers(Long postId) {
        List<Favorite> records = favoriteService.list(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
        return records.stream().map(Favorite::getUserId).collect(Collectors.toSet());
    }
}

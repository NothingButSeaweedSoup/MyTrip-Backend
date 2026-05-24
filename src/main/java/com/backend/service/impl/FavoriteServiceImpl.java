package com.backend.service.impl;

import com.backend.common.RedisKeys;
import com.backend.entity.Favorite;
import com.backend.entity.Post;
import com.backend.mapper.FavoriteMapper;
import com.backend.service.FavoriteService;
import com.backend.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private static final Logger redisLog = LoggerFactory.getLogger("redis.app");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PostService postService;

    @Override
    public long toggleFavorite(Long userId, Long postId, String action) {
        try {
            long result = toggleFavoriteWithRedis(userId, postId, action);
            redisLog.info("toggleFavorite post={} userId={} action={} result={} (Redis)", postId, userId, action, result);
            return result;
        } catch (Exception e) {
            redisLog.warn("Redis unavailable for toggleFavorite, fallback to DB: {}", e.getMessage());
            long result = toggleFavoriteWithDb(userId, postId, action);
            redisLog.info("toggleFavorite post={} userId={} action={} result={} (DB)", postId, userId, action, result);
            return result;
        }
    }

    private long toggleFavoriteWithRedis(Long userId, Long postId, String action) {
        String setKey = RedisKeys.FAVORITE_SET + postId;
        String countKey = RedisKeys.FAVORITE_COUNT + postId;
        String userKey = RedisKeys.FAVORITE_USER + userId;
        String uid = userId.toString();
        String pid = postId.toString();

        if ("favorite".equals(action)) {
            Long added = stringRedisTemplate.opsForSet().add(setKey, uid);
            if (added != null && added > 0) {
                stringRedisTemplate.opsForSet().add(userKey, pid);
                seedFavoriteCountFromDb(countKey, postId);
                long count = stringRedisTemplate.opsForValue().increment(countKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_FAVORITES, pid);
                return count;
            }
        } else if ("unfavorite".equals(action)) {
            Long removed = stringRedisTemplate.opsForSet().remove(setKey, uid);
            if (removed != null && removed > 0) {
                stringRedisTemplate.opsForSet().remove(userKey, pid);
                seedFavoriteCountFromDb(countKey, postId);
                long count = stringRedisTemplate.opsForValue().decrement(countKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_FAVORITES, pid);
                return count;
            }
        }
        String count = stringRedisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void seedFavoriteCountFromDb(String key, Long postId) {
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            long dbCount = count(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
            stringRedisTemplate.opsForValue().set(key, String.valueOf(dbCount));
        }
    }

    private long toggleFavoriteWithDb(Long userId, Long postId, String action) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getPostId, postId);

        if ("favorite".equals(action)) {
            if (count(wrapper) == 0) {
                Favorite fav = new Favorite();
                fav.setUserId(userId);
                fav.setPostId(postId);
                save(fav);
            }
        } else if ("unfavorite".equals(action)) {
            remove(wrapper);
        }
        return count(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        try {
            return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeys.FAVORITE_SET + postId, userId.toString())
            );
        } catch (Exception e) {
            return count(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getPostId, postId)) > 0;
        }
    }

    @Override
    public long getFavoriteCount(Long postId) {
        try {
            String val = stringRedisTemplate.opsForValue().get(RedisKeys.FAVORITE_COUNT + postId);
            if (val != null) return Long.parseLong(val);
        } catch (Exception ignored) {}
        return count(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
    }

    @Override
    public List<Long> getUserFavoriteIds(Long userId, int page, int pageSize) {
        try {
            String userKey = RedisKeys.FAVORITE_USER + userId;
            Set<String> members = stringRedisTemplate.opsForSet().members(userKey);
            if (members != null && !members.isEmpty()) {
                return members.stream()
                        .map(Long::parseLong)
                        .sorted((a, b) -> Long.compare(b, a))
                        .skip((long) (page - 1) * pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {}

        Page<Favorite> favPage = page(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        return favPage.getRecords().stream().map(Favorite::getPostId).collect(Collectors.toList());
    }
}

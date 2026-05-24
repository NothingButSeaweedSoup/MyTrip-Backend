package com.backend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.backend.common.BusinessException;
import com.backend.common.RedisKeys;
import com.backend.common.UnauthorizedException;
import com.backend.dto.*;
import com.backend.entity.*;
import com.backend.mapper.PostMapper;
import com.backend.service.*;
import com.backend.entity.PostAuditRecord;
import com.backend.task.ViewCountTask;
import com.backend.util.ImageUrlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final Logger redisLog = LoggerFactory.getLogger("redis.app");

    // Redis key constants → com.backend.common.RedisKeys

    @Autowired
    private PostImageService postImageService;

    @Autowired
    private PostTagService postTagService;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ViewCountTask viewCountTask;

    @Autowired
    private ImageUrlUtil imageUrlUtil;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Lazy
    @Autowired
    private PostAuditRecordService postAuditRecordService;

    @Lazy
    @Autowired
    private FavoriteService favoriteService;

    @Value("${upload.path:/data/uploads}")
    private String uploadPath;

    @Value("${app.images-path:sql/data/images}")
    private String imagesPath;

    @Override
    @Transactional
    public Long createPost(Long userId, PostCreateRequest request) {
        Post post = new Post();
        post.setAuthorId(userId);
        post.setHashId(generateHashId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setViews(0L);
        post.setLikes(0L);
        post.setCommentCount(0);
        post.setStatus(0); // 待审核
        save(post);

        // 保存图片到 hash 目录
        String hashId = post.getHashId();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            Path hashDir = Paths.get(imagesPath, hashId);
            try {
                Files.createDirectories(hashDir);
            } catch (IOException e) {
                throw new BusinessException("创建图片目录失败");
            }

            for (int i = 0; i < request.getImages().size(); i++) {
                String uploadedUrl = request.getImages().get(i);
                String filename = extractFilename(uploadedUrl);
                String ext = getFileExt(filename);
                String newName = hashId + "_" + String.format("%02d", i) + ext;

                try {
                    Path src = Paths.get(uploadPath, filename);
                    Path dst = hashDir.resolve(newName);
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    try { Files.deleteIfExists(src); } catch (IOException ignored) {}
                } catch (IOException e) {
                    log.warn("copy image failed: {} -> {}", filename, newName);
                    continue;
                }

                String dbUrl = "\\" + hashId + "\\" + newName;
                PostImage img = new PostImage();
                img.setPostId(post.getPostId());
                img.setUrl(dbUrl);
                img.setSortOrder(i);
                postImageService.save(img);
            }
        }

        // 保存标签关联
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            savePostTags(post.getPostId(), request.getTagIds());
        }

        return post.getPostId();
    }

    @Override
    public IPage<PostVO> listMyPosts(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, userId)
               .ne(Post::getStatus, 4)
               .orderByDesc(Post::getCreateTime);
        Page<Post> postPage = page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(p -> toPostVO(p, userId));
    }

    @Override
    public IPage<PostVO> listPosts(int page, int pageSize, String sortBy, String order) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);

        boolean sortByRedis = "views".equals(sortBy) || "likes".equals(sortBy);

        if (sortByRedis) {
            return listPostsSortedByRedis(page, pageSize, sortBy, order, wrapper);
        }

        wrapper.orderByDesc(Post::getCreateTime);
        Page<Post> postPage = page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(this::toPostVO);
    }

    private IPage<PostVO> listPostsSortedByRedis(int page, int pageSize, String sortBy, String order,
                                                  LambdaQueryWrapper<Post> wrapper) {
        List<Post> allPosts = list(wrapper);
        List<PostVO> vos = allPosts.stream().map(this::toPostVO).collect(Collectors.toList());

        Comparator<PostVO> comparator = "views".equals(sortBy)
                ? Comparator.comparingLong(PostVO::getViews)
                : Comparator.comparingLong(PostVO::getLikes);

        if ("asc".equalsIgnoreCase(order)) {
            vos.sort(comparator);
        } else {
            vos.sort(comparator.reversed());
        }

        int total = vos.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<PostVO> pageList = from < total ? vos.subList(from, to) : Collections.emptyList();

        Page<PostVO> result = new Page<>(page, pageSize, total);
        result.setRecords(pageList);
        return result;
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        return getPostDetail(postId, null);
    }

    @Override
    public PostVO getPostDetail(Long postId, Long userId) {
        Post post = getById(postId);
        if (post == null || post.getStatus() == 4) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != 1) {
            if (userId == null || !post.getAuthorId().equals(userId)) {
                throw new BusinessException("帖子不存在");
            }
        }
        viewCountTask.incrementViews(postId);
        return toPostVO(post, userId);
    }

    @Override
    @Transactional
    public void editPost(Long userId, Long postId, PostEditRequest request) {
        Post post = getById(postId);
        if (post == null || post.getStatus() == 4) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("无权编辑此帖子");
        }

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        post.setStatus(0); // 重新审核
        updateById(post);

        // 删除指定图片
        if (request.getImageIdsToDelete() != null && !request.getImageIdsToDelete().isEmpty()) {
            postImageService.removeBatchByIds(request.getImageIdsToDelete());
        }

        // 新增图片
        if (request.getImagesToAdd() != null && !request.getImagesToAdd().isEmpty()) {
            for (int i = 0; i < request.getImagesToAdd().size(); i++) {
                PostImage img = new PostImage();
                img.setPostId(postId);
                img.setUrl(request.getImagesToAdd().get(i));
                img.setSortOrder(i);
                postImageService.save(img);
            }
        }

        // 替换标签
        if (request.getTagIds() != null) {
            LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(PostTag::getPostId, postId);
            postTagService.remove(tagWrapper);
            savePostTags(postId, request.getTagIds());
        }
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getById(postId);
        if (post == null || post.getStatus() == 4) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("无权删除此帖子");
        }
        post.setStatus(4);
        updateById(post);
    }

    @Override
    public long toggleLike(Long userId, Long postId, String action) {
        try {
            long result = toggleLikeWithRedis(userId, postId, action);
            redisLog.info("toggleLike post={} action={} result={} (Redis)", postId, action, result);
            return result;
        } catch (Exception e) {
            redisLog.warn("Redis unavailable for toggleLike, fallback to DB: {}", e.getMessage());
            long result = toggleLikeWithDb(userId, postId, action);
            redisLog.info("toggleLike post={} action={} result={} (DB)", postId, action, result);
            return result;
        }
    }

    private long toggleLikeWithRedis(Long userId, Long postId, String action) {
        String likeKey = RedisKeys.POST_LIKES + postId;
        String likedSetKey = RedisKeys.POST_LIKED_SET + postId;

        if ("like".equals(action)) {
            Long added = stringRedisTemplate.opsForSet().add(likedSetKey, userId.toString());
            if (added != null && added > 0) {
                saveLikeRecord(userId, postId);
                seedCounterFromDb(likeKey, postId);
                long count = stringRedisTemplate.opsForValue().increment(likeKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_POSTS, postId.toString());
                return count;
            }
        } else if ("unlike".equals(action)) {
            Long removed = stringRedisTemplate.opsForSet().remove(likedSetKey, userId.toString());
            if (removed != null && removed > 0) {
                removeLikeRecord(userId, postId);
                seedCounterFromDb(likeKey, postId);
                long count = stringRedisTemplate.opsForValue().decrement(likeKey);
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_POSTS, postId.toString());
                return count;
            }
        }
        String count = stringRedisTemplate.opsForValue().get(likeKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void seedCounterFromDb(String key, Long postId) {
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            Post post = getById(postId);
            if (post != null) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(post.getLikes()));
            }
        }
    }

    private long toggleLikeWithDb(Long userId, Long postId, String action) {
        Post post = getById(postId);
        if (post == null) return 0;

        if ("like".equals(action)) {
            if (!hasLikeRecord(userId, postId)) {
                saveLikeRecord(userId, postId);
                post.setLikes(post.getLikes() + 1);
                updateById(post);
            }
        } else if ("unlike".equals(action)) {
            if (hasLikeRecord(userId, postId)) {
                removeLikeRecord(userId, postId);
                post.setLikes(Math.max(0, post.getLikes() - 1));
                updateById(post);
            }
        }
        return post.getLikes();
    }

    // ---- private helpers ----

    private void savePostTags(Long postId, List<Integer> tagIds) {
        for (Integer tagId : tagIds) {
            PostTag pt = new PostTag();
            pt.setPostId(postId);
            pt.setTagId(tagId);
            pt.setCreateTime(new Date());
            postTagService.save(pt);
        }
    }

    @Override
    public PostVO toPostVO(Post post) {
        return toPostVO(post, null);
    }

    @Override
    public PostVO toPostVO(Post post, Long userId) {
        // 查询作者
        User author = userService.getById(post.getAuthorId());

        // 查询图片
        LambdaQueryWrapper<PostImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(PostImage::getPostId, post.getPostId()).orderByAsc(PostImage::getSortOrder);
        List<PostImage> images = postImageService.list(imgWrapper);
        List<String> imageUrls = images.stream()
                .map(PostImage::getUrl)
                .map(imageUrlUtil::getFullUrl)
                .collect(Collectors.toList());

        // 查询标签
        LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(PostTag::getPostId, post.getPostId());
        List<PostTag> postTags = postTagService.list(tagWrapper);
        List<PostVO.TagInfo> tagInfos = new ArrayList<>();
        if (!postTags.isEmpty()) {
            List<Integer> tagIds = postTags.stream().map(PostTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            Map<Integer, String> tagNameMap = tags.stream()
                    .collect(Collectors.toMap(Tag::getTagId, Tag::getName));
            tagInfos = postTags.stream().map(pt -> PostVO.TagInfo.builder()
                    .tagId(pt.getTagId())
                    .name(tagNameMap.getOrDefault(pt.getTagId(), ""))
                    .build()).collect(Collectors.toList());
        }

        long views = getRedisLong(RedisKeys.POST_VIEWS + post.getPostId(), post.getViews());
        long likes = getRedisLong(RedisKeys.POST_LIKES + post.getPostId(), post.getLikes());
        boolean liked = userId != null && isLikedByUser(post.getPostId(), userId);
        boolean favorited = userId != null && favoriteService.isFavorited(userId, post.getPostId());

        return PostVO.builder()
                .postId(post.getPostId())
                .hashId(post.getHashId())
                .title(post.getTitle())
                .content(post.getContent())
                .views(views)
                .likes(likes)
                .liked(liked)
                .favorited(favorited)
                .status(post.getStatus())
                .rejectReason(getRejectReason(post.getPostId()))
                .commentCount(post.getCommentCount())
                .hotScore(post.getHotScore())
                .createTime(post.getCreateTime())
                .authorId(post.getAuthorId())
                .authorName(author != null ? author.getUsername() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .images(imageUrls)
                .tags(tagInfos)
                .build();
    }

    private String generateHashId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String extractFilename(String url) {
        String s = url.replace('\\', '/');
        int idx = s.lastIndexOf('/');
        return idx >= 0 ? s.substring(idx + 1) : s;
    }

    private String getFileExt(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }

    private boolean isLikedByUser(Long postId, Long userId) {
        try {
            return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeys.POST_LIKED_SET + postId, userId.toString())
            );
        } catch (Exception e) {
            return hasLikeRecord(userId, postId);
        }
    }

    private boolean hasLikeRecord(Long userId, Long postId) {
        LambdaQueryWrapper<UserBehavior> w = new LambdaQueryWrapper<>();
        w.eq(UserBehavior::getUserId, userId)
         .eq(UserBehavior::getPostId, postId)
         .eq(UserBehavior::getActionType, "like");
        return userBehaviorService.count(w) > 0;
    }

    private void saveLikeRecord(Long userId, Long postId) {
        if (hasLikeRecord(userId, postId)) return;
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setPostId(postId);
        ub.setActionType("like");
        userBehaviorService.save(ub);
    }

    private void removeLikeRecord(Long userId, Long postId) {
        LambdaQueryWrapper<UserBehavior> w = new LambdaQueryWrapper<>();
        w.eq(UserBehavior::getUserId, userId)
         .eq(UserBehavior::getPostId, postId)
         .eq(UserBehavior::getActionType, "like");
        userBehaviorService.remove(w);
    }

    @Override
    public List<PostVO> listPostsByIds(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Post> posts = listByIds(ids);
        Map<Long, Post> postMap = posts.stream()
                .filter(p -> p.getStatus() == 1)
                .collect(Collectors.toMap(Post::getPostId, p -> p));
        return ids.stream()
                .filter(postMap::containsKey)
                .map(id -> toPostVO(postMap.get(id), userId))
                .collect(Collectors.toList());
    }

    private String getRejectReason(Long postId) {
        PostAuditRecord record = postAuditRecordService.lambdaQuery()
                .eq(PostAuditRecord::getPostId, postId)
                .eq(PostAuditRecord::getAction, 2)
                .orderByDesc(PostAuditRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
        return record != null ? record.getReason() : null;
    }

    private long getRedisLong(String key, long fallback) {
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val != null) {
                redisLog.info("getRedisLong key={} redis={} fallback={}", key, val, fallback);
                return Long.parseLong(val);
            }
            redisLog.info("getRedisLong key={} miss, using fallback={}", key, fallback);
            return fallback;
        } catch (Exception e) {
            redisLog.warn("getRedisLong key={} error, using fallback={}: {}", key, fallback, e.getMessage());
            return fallback;
        }
    }

    @Override
    public IPage<Post> listPendingPosts(int page, int pageSize) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0)
               .orderByDesc(Post::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Post getNextPendingPost(Set<Long> excludeIds) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0)
               .orderByAsc(Post::getCreateTime);
        if (excludeIds != null && !excludeIds.isEmpty()) {
            wrapper.notIn(Post::getPostId, excludeIds);
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}

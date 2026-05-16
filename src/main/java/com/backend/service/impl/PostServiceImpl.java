package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.dto.*;
import com.backend.entity.*;
import com.backend.mapper.PostMapper;
import com.backend.service.*;
import com.backend.task.ViewCountTask;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

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

        // 保存图片
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                PostImage img = new PostImage();
                img.setPostId(post.getPostId());
                img.setUrl(request.getImages().get(i));
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
    public IPage<PostVO> listPosts(int page, int pageSize, String sortBy, String order) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1); // 仅审核通过

        if ("views".equals(sortBy)) {
            wrapper.orderByDesc(Post::getViews);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(Post::getLikes);
        } else {
            wrapper.orderByDesc(Post::getCreateTime);
        }

        Page<Post> postPage = page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(this::toPostVO);
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        Post post = getById(postId);
        if (post == null || post.getStatus() == 4) {
            throw new BusinessException("帖子不存在");
        }
        viewCountTask.incrementViews(postId);
        return toPostVO(post);
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
        String likeKey = "post:likes:" + postId;
        String likedSetKey = "post:liked:" + postId;

        if ("like".equals(action)) {
            Long added = stringRedisTemplate.opsForSet().add(likedSetKey, userId.toString());
            if (added != null && added > 0) {
                return stringRedisTemplate.opsForValue().increment(likeKey);
            }
        } else if ("unlike".equals(action)) {
            Long removed = stringRedisTemplate.opsForSet().remove(likedSetKey, userId.toString());
            if (removed != null && removed > 0) {
                return stringRedisTemplate.opsForValue().decrement(likeKey);
            }
        }
        String count = stringRedisTemplate.opsForValue().get(likeKey);
        return count != null ? Long.parseLong(count) : 0L;
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
        // 查询作者
        User author = userService.getById(post.getAuthorId());

        // 查询图片
        LambdaQueryWrapper<PostImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(PostImage::getPostId, post.getPostId()).orderByAsc(PostImage::getSortOrder);
        List<PostImage> images = postImageService.list(imgWrapper);
        List<String> imageUrls = images.stream().map(PostImage::getUrl).collect(Collectors.toList());

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

        return PostVO.builder()
                .postId(post.getPostId())
                .hashId(post.getHashId())
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .likes(post.getLikes())
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
}

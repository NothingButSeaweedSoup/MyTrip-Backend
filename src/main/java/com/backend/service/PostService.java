package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.Post;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostService extends IService<Post> {

    /** 获取当前用户的帖子列表 */
    IPage<PostVO> listMyPosts(Long userId, int page, int pageSize);

    /** 发布帖子，返回帖子ID */
    Long createPost(Long userId, PostCreateRequest request);

    /** 获取帖子列表（仅审核通过的） */
    IPage<PostVO> listPosts(int page, int pageSize, String sortBy, String order);

    /** 获取帖子详情 */
    PostVO getPostDetail(Long postId);

    /** 获取帖子详情（含当前用户点赞状态） */
    PostVO getPostDetail(Long postId, Long userId);

    /** 编辑帖子 */
    void editPost(Long userId, Long postId, PostEditRequest request);

    /** 删除帖子（软删除） */
    void deletePost(Long userId, Long postId);

    /** 点赞/取消点赞 */
    long toggleLike(Long userId, Long postId, String action);

    /** 将 Post 实体转为 PostVO（供其他模块复用） */
    PostVO toPostVO(com.backend.entity.Post post);

    /** 将 Post 实体转为 PostVO，同时查询当前用户是否已点赞 */
    PostVO toPostVO(com.backend.entity.Post post, Long userId);

    /** 根据ID列表获取帖子，保持传入顺序，跳过已删除/不存在的帖子 */
    java.util.List<PostVO> listPostsByIds(java.util.List<Long> ids, Long userId);

    /** 待审核帖子列表 */
    IPage<Post> listPendingPosts(int page, int pageSize);

    /** 获取下一个待审核帖子（排除已锁定的），按创建时间升序 */
    Post getNextPendingPost(java.util.Set<Long> excludeIds);
}

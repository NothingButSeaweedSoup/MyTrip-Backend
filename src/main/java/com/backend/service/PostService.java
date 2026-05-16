package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.Post;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostService extends IService<Post> {

    /** 发布帖子，返回帖子ID */
    Long createPost(Long userId, PostCreateRequest request);

    /** 获取帖子列表（仅审核通过的） */
    IPage<PostVO> listPosts(int page, int pageSize, String sortBy, String order);

    /** 获取帖子详情 */
    PostVO getPostDetail(Long postId);

    /** 编辑帖子 */
    void editPost(Long userId, Long postId, PostEditRequest request);

    /** 删除帖子（软删除） */
    void deletePost(Long userId, Long postId);

    /** 点赞/取消点赞 */
    long toggleLike(Long userId, Long postId, String action);
}

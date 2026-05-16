package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.Post;
import com.backend.service.PostService;
import com.backend.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【post(帖子表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{

}





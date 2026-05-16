package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.PostTag;
import com.backend.service.PostTagService;
import com.backend.mapper.PostTagMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【post_tag(帖子-标签关联表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class PostTagServiceImpl extends ServiceImpl<PostTagMapper, PostTag>
    implements PostTagService{

}





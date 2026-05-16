package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.PostImage;
import com.backend.service.PostImageService;
import com.backend.mapper.PostImageMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【post_image(帖子图片表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class PostImageServiceImpl extends ServiceImpl<PostImageMapper, PostImage>
    implements PostImageService{

}





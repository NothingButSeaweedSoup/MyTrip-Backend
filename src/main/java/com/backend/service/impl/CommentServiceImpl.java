package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.Comment;
import com.backend.service.CommentService;
import com.backend.mapper.CommentMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【comment(评论表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

}





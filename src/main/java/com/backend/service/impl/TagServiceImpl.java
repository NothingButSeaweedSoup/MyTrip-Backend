package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.Tag;
import com.backend.service.TagService;
import com.backend.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【tag(标签表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}





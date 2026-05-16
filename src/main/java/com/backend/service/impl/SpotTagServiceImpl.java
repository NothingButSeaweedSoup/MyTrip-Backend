package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.SpotTag;
import com.backend.service.SpotTagService;
import com.backend.mapper.SpotTagMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【spot_tag(景点-标签关联表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class SpotTagServiceImpl extends ServiceImpl<SpotTagMapper, SpotTag>
    implements SpotTagService{

}





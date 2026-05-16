package com.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.PostAuditRecord;
import com.backend.service.PostAuditRecordService;
import com.backend.mapper.PostAuditRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【post_audit_record(统一审核记录表)】的数据库操作Service实现
* @createDate 2026-05-16 00:16:36
*/
@Service
public class PostAuditRecordServiceImpl extends ServiceImpl<PostAuditRecordMapper, PostAuditRecord>
    implements PostAuditRecordService{

}





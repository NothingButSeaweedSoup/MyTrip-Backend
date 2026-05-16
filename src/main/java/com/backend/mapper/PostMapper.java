package com.backend.mapper;

import com.backend.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT * FROM post WHERE hash_id = #{hashId}")
    Post selectByHashId(@Param("hashId") String hashId);

    @Update("UPDATE post SET views = views + 1 WHERE post_id = #{postId}")
    int incrementViews(@Param("postId") Long postId);
}





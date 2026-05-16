package com.backend.mapper;

import com.backend.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);
}





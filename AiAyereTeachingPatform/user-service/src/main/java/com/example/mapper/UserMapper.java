package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /*
    int insert(User user);
    User selectById(Serializable id);
    int deleteById(Serializable id);
    int updateById(User user);
    List<User> selectList();
     */
}

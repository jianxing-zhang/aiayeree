package com.example.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User getUserByphone(String phone) {
        return getUserByphone(phone);
    }

    // ===================== 修复：只返回第一条 =====================
    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getName, username);
        List<User> list = userMapper.selectList(queryWrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public User getUsername(String id) {
        return getById(id);
    }

    @Override
    public boolean login(String username, String password) {
        User user = getUserByUsername(username);
        return user != null && user.getPassword().equals(password);
    }

    public List<User> listAll() {
        return list();
    }

    public boolean add(User user) {
        return save(user);
    }

    public boolean update(User user) {
        return updateById(user);
    }

    public boolean delete(Integer id) {
        return removeById(id);
    }
    @Override
    public User getUserByUsernameAndPhone(String username, String phone) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getName, username)
                .eq(User::getPhone, phone);
        return this.getOne(wrapper);
    }

}
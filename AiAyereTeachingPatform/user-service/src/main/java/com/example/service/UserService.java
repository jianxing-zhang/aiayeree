package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.User;


import java.util.List;

public interface UserService extends IService<User> {
    User getUserByUsername(String username);
    User getUsername(String id);
    boolean login(String username, String password);
    List<User> listAll();
    boolean add(User user);
    boolean update(User user);
    boolean delete(Integer id);
    User getUserByphone(String phone);
    User getUserByUsernameAndPhone(String username, String phone);


}
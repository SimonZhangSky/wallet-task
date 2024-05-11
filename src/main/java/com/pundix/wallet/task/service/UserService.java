package com.pundix.wallet.task.service;

import com.pundix.wallet.task.dao.UserMapper;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.repository.UserRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.util.List;

@Service
public class UserService {

    // 简单CURD使用JPA
    private final UserRepository userRepository;

    // 复杂SQL语句使用MyBatis
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User createUser(String name) {
        User user = userRepository.findByName(name);
        if (user != null) {
            throw new EntityExistsException("用户已存在");
        }

        user = new User();
        user.setName(name);
        return userRepository.save(user);
    }

}

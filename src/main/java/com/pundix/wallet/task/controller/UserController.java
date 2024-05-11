package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.service.UserService;
import com.pundix.wallet.task.utils.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("list")
    public ApiResponse listUsers() {
        List<User> users = userService.getUsers();

        return ApiResponse.success("查询成功", users);
    }

    @PostMapping("create")
    public ApiResponse createUser(@RequestBody User createUser) {
        User user = userService.createUser(createUser.getName());

        return ApiResponse.success("创建成功", user);
    }


}

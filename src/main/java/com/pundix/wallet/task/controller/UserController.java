package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.dto.UserCreateRequest;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.service.UserService;
import com.pundix.wallet.task.utils.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@Api(value = "用户管理", tags = "用户管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("list")
    @ApiOperation("查询用户列表")
    public ApiResponse listUsers() {
        List<User> users = userService.getUsers();

        return ApiResponse.success("查询成功", users);
    }

    @PostMapping("create")
    @ApiOperation(value = "创建用户")
    @ApiImplicitParam(name = "user", value = "创建用户", required = true)
    public ApiResponse createUser(@RequestBody UserCreateRequest createUser) {
        User user = userService.createUser(createUser.getName());

        return ApiResponse.success("创建成功", user);
    }

}

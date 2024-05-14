package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.dto.UserWalletResponse;
import com.pundix.wallet.task.service.UserWalletService;
import com.pundix.wallet.task.utils.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

@RestController
@RequestMapping("/userWallet")
@Api(value = "用户钱包管理", tags = "用户钱包管理")
public class UserWalletController {

    private final UserWalletService userWalletService;

    public UserWalletController(UserWalletService userWalletService) {
        this.userWalletService = userWalletService;
    }

    @GetMapping("/{userId}/list")
    @ApiOperation("查询用户钱包列表")
    @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Integer", paramType = "path")
    public ApiResponse listUserWallets(@PathVariable Integer userId) {
        List<UserWalletResponse> allUserWallets = userWalletService.getAllUserWallets(userId);

        return ApiResponse.success("查询成功", allUserWallets);
    }

    @GetMapping("/{userId}/balance")
    @ApiOperation("根据地址查询用户钱包余额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Integer", paramType = "path"),
            @ApiImplicitParam(name = "address", value = "地址", required = true, dataType = "String", paramType = "query")
    })
    public ApiResponse getUserBalance(@PathVariable Integer userId, @RequestParam("address") String address) {
        BigInteger balance = userWalletService.getUserBalance(userId, address);

        return ApiResponse.success("查询成功", balance);
    }

    @PostMapping("/{userId}/create")
    @ApiOperation(value = "创建用户钱包地址")
    @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Integer", paramType = "path")
    public ApiResponse createUser(@PathVariable Integer userId) {
        UserWalletResponse walletResponse = userWalletService.createWallet(userId);

        return ApiResponse.success("创建成功", walletResponse);
    }
}

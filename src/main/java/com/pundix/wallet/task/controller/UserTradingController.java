package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.dto.UserTradingSendETHRequest;
import com.pundix.wallet.task.dto.UserTradingSendTokenRequest;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
import com.pundix.wallet.task.service.UserTradingService;
import com.pundix.wallet.task.utils.ApiResponse;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userTrading")
@Api(value = "用户交易管理", tags = "用户交易管理")
public class UserTradingController {

    private final UserTradingService userTradingService;

    public UserTradingController(UserTradingService userTradingService) {
        this.userTradingService = userTradingService;
    }

    @PostMapping("/{userId}/sendETH")
    @ApiOperation("用户发送ETH")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Integer", paramType = "path"),
            @ApiImplicitParam(name = "sendETHRequest", value = "发送ETH请求", required = true, dataType = "UserTradingSendETHRequest", paramType = "body")
    })
    public ApiResponse sendETH(@PathVariable Integer userId, @RequestBody UserTradingSendETHRequest sendETHRequest) {

        String txHash = userTradingService.sendETH(userId, sendETHRequest);

        return ApiResponse.success("交易成功", txHash);
    }

    @PostMapping("/{userId}/sendToken")
    @ApiOperation("用户发送Token")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Integer", paramType = "path"),
            @ApiImplicitParam(name = "sendTokenRequest", value = "发送Token请求", required = true, dataType = "UserTradingSendTokenRequest", paramType = "body")
    })
    public ApiResponse sendToken(@PathVariable Integer userId, @RequestBody UserTradingSendTokenRequest sendTokenRequest) {

        String txHash = userTradingService.sendToken(userId, sendTokenRequest);

        return ApiResponse.success("交易成功", txHash);
    }

    @PostMapping("/{userId}/transaction/list")
    public ApiResponse transactionList(@PathVariable Integer userId, @RequestBody UserTransactionListRequest transactionListRequest) {

        // TODO 交易记录查询

        return ApiResponse.success("查询成功", null);
    }

}

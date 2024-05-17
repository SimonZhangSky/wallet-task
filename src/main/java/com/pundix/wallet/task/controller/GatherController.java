package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.service.GatherETHService;
import com.pundix.wallet.task.utils.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gather")
@Api(value = "归集管理", tags = "归集管理")
public class GatherController {

    private final GatherETHService gatherETHService;

    public GatherController(GatherETHService gatherETHService) {
        this.gatherETHService = gatherETHService;
    }

    @GetMapping("/eth/start")
    @ApiOperation("手动开启归集ETH")
    public ApiResponse ethStart() {
        gatherETHService.gatherETH();

        return ApiResponse.success("开始归集ETH", null);
    }

    @GetMapping("/usdt/start")
    @ApiOperation("手动开启归集ETH")
    public ApiResponse usdtStart() {


        return ApiResponse.success("开始归集USDT", null);
    }

}

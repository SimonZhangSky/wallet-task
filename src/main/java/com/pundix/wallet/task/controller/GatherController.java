package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.service.GatherETHService;
import com.pundix.wallet.task.service.GatherUSDTService;
import com.pundix.wallet.task.utils.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gather")
@Api(value = "归集管理", tags = "归集管理")
public class GatherController {

    private final GatherETHService gatherETHService;

    private final GatherUSDTService gatherUSDTService;

    public GatherController(GatherETHService gatherETHService, GatherUSDTService gatherUSDTService) {
        this.gatherETHService = gatherETHService;
        this.gatherUSDTService = gatherUSDTService;
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
        gatherUSDTService.gatherUSDT();

        return ApiResponse.success("开始归集USDT", null);
    }

}

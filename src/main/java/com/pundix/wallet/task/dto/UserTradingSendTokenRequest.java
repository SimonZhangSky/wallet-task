package com.pundix.wallet.task.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("用户交易发送代币请求")
public class UserTradingSendTokenRequest {

    @ApiModelProperty(value = "用户钱包ID", required = true, example = "0")
    private Integer userWalletId;

    @ApiModelProperty(value = "接收地址", required = true, example = "0x111")
    private String toAddress;

    @ApiModelProperty(value = "金额", required = true, example = "0.1")
    private BigDecimal amount;

    @ApiModelProperty(value = "代币地址", required = true, example = "0x222")
    private String tokenAddress;

}

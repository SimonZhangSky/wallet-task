package com.pundix.wallet.task.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

@Data
@ApiModel("用户查询交易请求")
public class UserTransactionListRequest extends BasePageRequest {

    @ApiModelProperty(value = "发送地址", required = true, example = "0x111")
    private String fromAddress;

    @ApiModelProperty(value = "开始区块高度", example = "0")
    private BigInteger startBlock;

    @ApiModelProperty(value = "结束区块高度", example = "0")
    private BigInteger endBlock;

    @ApiModelProperty(value = "币种", required = true, example = "ETH")
    private String coinType;

    @ApiModelProperty(value = "接收地址", required = true, example = "0x111")
    private String toAddress;

    @ApiModelProperty(value = "代币地址", required = true, example = "0x222")
    private String tokenAddress;

}

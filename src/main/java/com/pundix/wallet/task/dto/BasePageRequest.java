package com.pundix.wallet.task.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BasePageRequest {

    @ApiModelProperty(value = "页码", required = true, example = "1")
    private int page = 1;

    @ApiModelProperty(value = "每页数量", required = true, example = "10")
    private int pageSize = 10;

}

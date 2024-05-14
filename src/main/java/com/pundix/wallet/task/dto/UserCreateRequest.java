package com.pundix.wallet.task.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户创建请求")
public class UserCreateRequest {

    @ApiModelProperty(value = "用户名", required = true, example = "A")
    private String name;

}

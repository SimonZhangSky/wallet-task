package com.pundix.wallet.task.enums;

import lombok.Getter;

@Getter
public enum WalletStatusEnum {

    NORMAL(1),
    FROZEN(2),
    DELETED(3)
    ;

    private final int status;

    WalletStatusEnum(int status) {
        this.status = status;
    }

}

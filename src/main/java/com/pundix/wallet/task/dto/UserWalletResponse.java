package com.pundix.wallet.task.dto;

import com.pundix.wallet.task.enums.WalletStatusEnum;
import com.pundix.wallet.task.enums.WalletTpyeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWalletResponse {

    private int walletId;

    @Enumerated(EnumType.STRING)
    private WalletTpyeEnum walletType;

    private String address;

    private String privateKey;

    private String publicKey;

    @Enumerated(EnumType.ORDINAL)
    private WalletStatusEnum status;

    private String remark;

    private boolean isDefault;

    private BigInteger balanceWei;

    private BigDecimal balanceEther;

}

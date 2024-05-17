package com.pundix.wallet.task.entity;

import com.pundix.wallet.task.enums.WalletStatusEnum;
import com.pundix.wallet.task.enums.WalletTpyeEnum;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "user_wallet")
@Data
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false, columnDefinition = "int COMMENT '用户ID'")
    private int userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, columnDefinition = "varchar(100) COMMENT '钱包类型'")
    private WalletTpyeEnum walletType;

    @Column(name = "address", nullable = false, length = 100, columnDefinition = "varchar(100) COMMENT '钱包地址'")
    private String address;

    @Column(name = "private_key", nullable = false, length = 500, columnDefinition = "varchar(500) COMMENT '私钥'")
    private String privateKey;

    @Column(name = "public_key", nullable = false, length = 500, columnDefinition = "varchar(500) COMMENT '公钥'")
    private String publicKey;

    @Column(name = "balance_wei", nullable = false, columnDefinition = "bigint COMMENT '余额Wei'")
    private BigInteger balanceWei;

    @Column(name = "balance_ether", nullable = false, columnDefinition = "decimal(20, 10) COMMENT '余额Ether'")
    private BigDecimal balanceEther;

    @Column(name = "gather_amount_wei", nullable = false, columnDefinition = "bigint COMMENT '归集金额Wei'")
    private BigInteger gatherAmountWei;

    @Column(name = "gather_amount_ether", nullable = false, columnDefinition = "decimal(20, 10) COMMENT '归集金额Ether'")
    private BigDecimal gatherAmountEther;

    @Column(name = "create_time", nullable = false, columnDefinition = "datetime COMMENT '创建时间'")
    private Date createTime;

    @Column(name = "update_time", nullable = false, columnDefinition = "datetime COMMENT '更新时间'")
    private Date updateTime;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, columnDefinition = "int DEFAULT 1 COMMENT '状态'")
    private WalletStatusEnum status;

    @Column(name = "remark", length = 100, columnDefinition = "varchar(100) COMMENT '备注'")
    private String remark;

    @Column(name = "is_default", columnDefinition = "boolean DEFAULT false COMMENT '是否默认'")
    private boolean isDefault;

}

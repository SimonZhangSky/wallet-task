package com.pundix.wallet.task.entity;

import com.pundix.wallet.task.enums.TransactionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "gather_gas_record")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GatherGasRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false, columnDefinition = "int COMMENT '用户ID'")
    private int userId;

    @Column(name = "user_wallet_id", nullable = false, columnDefinition = "int COMMENT '用户钱包ID'")
    private int userWalletId;

    @Column(name = "tx_hash", nullable = false, length = 500, unique = true, columnDefinition = "varchar(500) COMMENT '交易Hash'")
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(50) COMMENT '交易状态'")
    private TransactionStatusEnum status;

    @Column(name = "gas_amount_wei", nullable = false, columnDefinition = "bigint COMMENT '发送Gas金额Wei'")
    private BigInteger gasAmountWei;

    @Column(name = "gas_amount_ether", nullable = false, columnDefinition = "decimal(20, 10) COMMENT '发送Gas金额Ether'")
    private BigDecimal gasAmountEther;

    @Column(name = "create_time", nullable = false, columnDefinition = "datetime COMMENT '创建时间'")
    private Date createTime;

    @Column(name = "update_time", nullable = false, columnDefinition = "datetime COMMENT '修改时间'")
    private Date updateTime;
}

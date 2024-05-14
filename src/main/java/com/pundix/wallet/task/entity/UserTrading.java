package com.pundix.wallet.task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "user_trading")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserTrading implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false, columnDefinition = "int COMMENT '用户ID'")
    private int userId;

    @Column(name = "user_wallet_id", nullable = false, columnDefinition = "int COMMENT '用户钱包ID'")
    private int userWalletId;

    @Column(name = "tx_hash", nullable = false, length = 500, unique = true, columnDefinition = "varchar(500) COMMENT '交易Hash'")
    private String txHash;

    @Column(name = "create_time", nullable = false, columnDefinition = "datetime COMMENT '创建时间'")
    private Date createTime;
}

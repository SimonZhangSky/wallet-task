package com.pundix.wallet.task.entity;

import com.pundix.wallet.task.enums.TransactionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "transaction_record")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "from_user_id", columnDefinition = "int COMMENT '发送方用户ID'")
    private Integer fromUserId;

    @Column(name = "from_user_wallet_id", columnDefinition = "int COMMENT '发送方用户钱包ID'")
    private Integer fromUserWalletId;

    @Column(name="from_address", nullable = false, length = 100, columnDefinition = "varchar(100) COMMENT '发送方地址'")
    private String fromAddress;

    @Column(name = "to_user_id", columnDefinition = "int COMMENT '接收方用户ID'")
    private Integer toUserId;

    @Column(name = "to_user_wallet_id", columnDefinition = "int COMMENT '接收方用户钱包ID'")
    private Integer toUserWalletId;

    @Column(name="to_address", nullable = false, length = 100, columnDefinition = "varchar(100) COMMENT '接收方地址'")
    private String toAddress;

    @Column(name = "tx_hash", nullable = false, length = 500, unique = true, columnDefinition = "varchar(500) COMMENT '交易Hash'")
    private String txHash;

    @Column(name = "block_number", nullable = false, columnDefinition = "bigint COMMENT '区块号'")
    private BigInteger blockNumber;

    @Column(name = "nonce", nullable = false, columnDefinition = "bigint COMMENT '交易序号'")
    private BigInteger nonce;

    @Column(name = "gas_price", nullable = false, columnDefinition = "bigint COMMENT 'Gas价格'")
    private BigInteger gasPrice;

    @Column(name = "gas_limit", nullable = false, columnDefinition = "bigint COMMENT 'Gas限制'")
    private BigInteger gasLimit;

    @Column(name = "gas_used", nullable = false, columnDefinition = "bigint COMMENT 'Gas使用量'")
    private BigInteger gasUsed;

    @Column(name = "value", nullable = false, columnDefinition = "bigint COMMENT '交易金额'")
    private BigInteger value;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(50) COMMENT '交易状态'")
    private TransactionStatusEnum status;

    @Column(name = "input", columnDefinition = "text COMMENT '合约交易输入'")
    private String input;

    @Column(name = "function_info", columnDefinition = "text COMMENT '合约交易方法信息'")
    private String functionInfo;

    @Column(name = "creates", length = 100, columnDefinition = "varchar(100) COMMENT '合约地址'")
    private String creates;

    @Column(name = "public_key", length = 500, columnDefinition = "varchar(500) COMMENT '公钥'")
    private String publicKey;

    @Column(name = "create_time", nullable = false, columnDefinition = "datetime COMMENT '创建时间'")
    private Date createTime;

    @Column(name = "update_time", nullable = false, columnDefinition = "datetime COMMENT '更新时间'")
    private Date updateTime;
}

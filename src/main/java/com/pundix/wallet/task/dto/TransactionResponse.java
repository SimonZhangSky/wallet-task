package com.pundix.wallet.task.dto;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;

public class TransactionResponse {

    private String hash;
    private BigInteger timestamp;
    private String from;
    private String to;
    private BigInteger gas;
    private BigInteger gasPrice;
    private BigInteger value;

}

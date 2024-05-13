package com.pundix.wallet.task.core.eth;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 以太坊链操作
 */
@Component
public class EthChain {

    private final Web3j web3j;

    private final Admin admin;

    public EthChain(Web3j web3j, Admin admin) {
        this.web3j = web3j;
        this.admin = admin;
    }

    // 默认 Gas 价格
    public final BigDecimal defaultGasPrice = BigDecimal.valueOf(5);

    /**
     * 生成地址
     *
     * @return 钱包地址、私钥、公钥
     */
    public Triple<String, BigInteger, BigInteger> generateAddress() {
        // 生成新的密钥对
        ECKeyPair ecKeyPair;
        try {
            ecKeyPair = Keys.createEcKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成TH地址失败", e);
        }
        // 获取私钥和公钥
        BigInteger privateKey = ecKeyPair.getPrivateKey();
        BigInteger publicKey = ecKeyPair.getPublicKey();
        // 获取钱包地址
        String address = Keys.getAddress(publicKey);

        return Triple.of(address, privateKey, publicKey);
    }

    /**
     * 获取余额
     *
     * @param address 钱包地址
     * @return 余额
     */
    public BigInteger getBalanceByAddress(String address) {
        BigInteger balance;
        try {
            EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
            balance = ethGetBalance.getBalance();
        } catch (Exception e) {
            throw new RuntimeException("获取ETH地址余额失败", e);
        }
        System.out.println("address " + address + " balance " + balance + "wei");
        return balance;
    }

    /**
     * 生成一个普通交易对象
     *
     * @param fromAddress 放款方
     * @param toAddress   收款方
     * @param nonce       交易序号
     * @param gasPrice    gas 价格
     * @param gasLimit    gas 数量
     * @param value       金额
     * @return 交易对象
     */
    public Transaction generateTransaction(String fromAddress, String toAddress,
                                               BigInteger nonce, BigInteger gasPrice,
                                               BigInteger gasLimit, BigInteger value) {
        Transaction transaction;
        transaction = Transaction.createEtherTransaction(fromAddress, nonce, gasPrice, gasLimit, toAddress, value);
        return transaction;
    }

    /**
     * 获取普通交易的gas上限
     *
     * @param transaction 交易对象
     * @return gas 上限
     */
    private BigInteger getTransactionGasLimit(Transaction transaction) {
        BigInteger gasLimit = BigInteger.ZERO;
        try {
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();
            gasLimit = ethEstimateGas.getAmountUsed();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gasLimit;
    }

    /**
     * 获取账号交易次数 nonce
     *
     * @param address 钱包地址
     * @return nonce
     */
    private BigInteger getTransactionNonce(String address) {
        BigInteger nonce = BigInteger.ZERO;
        try {
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
            nonce = ethGetTransactionCount.getTransactionCount();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nonce;
    }

    /**
     * 发送一个普通交易
     *
     * @return 交易 Hash
     */
    public String sendTransaction(String userId, String toAddress, BigDecimal amount) {
        // TODO 根据用户ID获取用户的私钥
        String fromAddress = userId + "1";
        String password = "123";
        BigInteger unlockDuration = BigInteger.valueOf(60L);
        String txHash = null;
        try {
            PersonalUnlockAccount personalUnlockAccount = admin.personalUnlockAccount(fromAddress, password, unlockDuration).send();
            if (personalUnlockAccount.accountUnlocked()) {
                BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
                Transaction transaction = generateTransaction(fromAddress, toAddress,
                        null, null, null, value);
                //不是必须的 可以使用默认值
                BigInteger gasLimit = getTransactionGasLimit(transaction);
                //不是必须的 缺省值就是正确的值
                BigInteger nonce = getTransactionNonce(fromAddress);
                //该值为大部分矿工可接受的gasPrice
                BigInteger gasPrice = Convert.toWei(defaultGasPrice, Convert.Unit.GWEI).toBigInteger();
                transaction = generateTransaction(fromAddress, toAddress,
                        nonce, gasPrice,
                        gasLimit, value);
                EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).send();
                txHash = ethSendTransaction.getTransactionHash();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("tx hash " + txHash);
        return txHash;
    }

}

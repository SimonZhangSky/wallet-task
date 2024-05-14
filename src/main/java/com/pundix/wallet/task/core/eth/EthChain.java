package com.pundix.wallet.task.core.eth;

import com.pundix.wallet.task.entity.UserWallet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * 以太坊链操作
 */
@Component
@Slf4j
public class EthChain {

    private final Web3j web3j;

    public EthChain(Web3j web3j) {
        this.web3j = web3j;
    }

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
        Credentials credentials = Credentials.create(ecKeyPair);

        // 获取私钥和公钥
        BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
        BigInteger publicKey = credentials.getEcKeyPair().getPublicKey();
        // 获取钱包地址
        String address = credentials.getAddress();

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
     * 发送ETH交易
     *
     * @param userWallet  钱包
     * @param toAddress   收款方
     * @param amount      金额
     * @return 交易 Hash
     */
    public String sendETHTransaction(UserWallet userWallet, String toAddress,
                                           BigDecimal amount) {
        String txHash = null;
        try {
            // 获取凭证
            Credentials credentials = Credentials.create(userWallet.getPrivateKey());
            // 构建交易对象
            TransactionReceipt transactionReceipt = Transfer.sendFunds(
                    web3j,
                    credentials,
                    toAddress,
                    amount,
                    Convert.Unit.ETHER
            ).send();
            txHash = transactionReceipt.getTransactionHash();

            log.info("ETH Transaction Response: {}", transactionReceipt);
            log.info("ETH Transaction complete, view it at https://rinkeby.etherscan.io/tx/"
                    + txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return txHash;
    }

    /**
     * 发送一个Token交易
     *
     * @param userWallet   钱包
     * @param toAddress    收款地址
     * @param amount       金额
     * @param tokenAddress 代币地址
     *
     * @return 交易 Hash
     */
    public String sendTokenTransaction(UserWallet userWallet, String toAddress, BigDecimal amount, String tokenAddress) {
        String fromAddress = userWallet.getAddress();
        BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();

        String txHash = null;
        try {
            // 构造 ERC20 代币转账方法调用的函数对象
            Function function = new Function(
                    "transfer", // 方法名
                    List.of(new Address(toAddress), new Uint256(Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger())), // 方法参数
                    List.of()); // 输出参数
            String encodedFunction = FunctionEncoder.encode(function);

            // 构建交易对象
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    fromAddress,
                    getTransactionNonce(fromAddress),
                    Transaction.DEFAULT_GAS,
                    null,
                    tokenAddress,
                    value,
                    encodedFunction
            );

            log.info("Token Transaction: {}", transaction);

            // 发送交易并获取交易结果
            EthSendTransaction transactionResponse = web3j.ethSendTransaction(transaction).sendAsync().get();

            log.info("Token Transaction Response: {}", transactionResponse);

            txHash = transactionResponse.getTransactionHash();

            log.info("Token Transaction complete, view it at https://rinkeby.etherscan.io/tx/"
                    + txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("tx hash " + txHash);
        return txHash;
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
}

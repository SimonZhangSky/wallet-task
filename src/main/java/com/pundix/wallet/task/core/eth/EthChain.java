package com.pundix.wallet.task.core.eth;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
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
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
    public Triple<String, String, String> generateAddress() {
        // 生成新的密钥对
        ECKeyPair ecKeyPair;
        try {
            ecKeyPair = Keys.createEcKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成TH地址失败", e);
        }
        Credentials credentials = Credentials.create(ecKeyPair);

        // 获取私钥和公钥
        String privateKey = String.format("%064x", ecKeyPair.getPrivateKey());
        String publicKey = String.format("%0128x", ecKeyPair.getPublicKey());

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
            log.info("ETH Transaction complete, view it at https://sepolia.etherscan.io/tx/" + txHash);

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
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                    fromAddress,
                    getTransactionNonce(fromAddress),
                    org.web3j.protocol.core.methods.request.Transaction.DEFAULT_GAS,
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

            log.info("Token Transaction complete, view it at https://sepolia.etherscan.io/tx/" + txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("tx hash " + txHash);
        return txHash;
    }

    /**
     * 获取交易列表
     *
     * @param transactionListRequest 交易列表请求
     * @return 交易列表
     */
    public PageInfo<Transaction> getTransactionList(UserTransactionListRequest transactionListRequest) {
        int page = transactionListRequest.getPage();
        int pageSize = transactionListRequest.getPageSize();

        String fromAddress = transactionListRequest.getFromAddress();
        String tokenAddress = transactionListRequest.getCoinType().getTokenAddress();
        BigInteger startBlock = transactionListRequest.getStartBlock();
        BigInteger endBlock = transactionListRequest.getEndBlock();

        List<Transaction> allTransactions = new ArrayList<>();
        try {
            // 获取区块高度范围内的交易
            for (BigInteger i = startBlock; i.compareTo(endBlock) <= 0; i = i.add(BigInteger.ONE)) {
                EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(i), true).send().getBlock();
                List<EthBlock.TransactionResult> blockTransactions = block.getTransactions();

                for (EthBlock.TransactionResult txResult : blockTransactions) {
                    if (txResult instanceof EthBlock.TransactionObject) {
                        org.web3j.protocol.core.methods.response.Transaction tx = ((EthBlock.TransactionObject) txResult).get();

                        if (fromAddress.equalsIgnoreCase(tx.getFrom()) || fromAddress.equalsIgnoreCase(tx.getTo())) {
                            if (tokenAddress == null || tx.getTo().equalsIgnoreCase(tokenAddress)) {
                                allTransactions.add(tx);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 开始分页
        Page<Transaction> pageInfo = PageHelper.startPage(page, pageSize);
        // 将数据添加到 Page 对象
        pageInfo.addAll(allTransactions);

        return new PageInfo<>(pageInfo);
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


    public static void main(String[] args) throws IOException {
        // 使用 Infura 的 Sepolia 测试网络 URL
        String infuraProjectId = "d20d2f1b1f3b4eb7868f002272216072";
        Web3j web3j = Web3j.build(new HttpService("https://sepolia.infura.io/v3/" + infuraProjectId));

        // 要查询余额的以太坊地址
        String address = "0x3717d4D1C97bBFd9a5E543E9aa880F4AD9c88270";

        // 查询余额
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigInteger balance = ethGetBalance.getBalance();

        // 打印余额（以 wei 为单位）
        System.out.println("Balance in wei: " + balance);

        // 将余额转换为以太坊单位（ETH）
        BigDecimal balanceInEther = new BigDecimal(balance).divide(new BigDecimal("1000000000000000000"));
        System.out.println("Balance in Ether: " + balanceInEther);
    }
}

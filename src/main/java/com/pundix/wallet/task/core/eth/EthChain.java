package com.pundix.wallet.task.core.eth;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
import com.pundix.wallet.task.entity.UserWallet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

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
     * 获取代币余额
     *
     * @param address      钱包地址
     * @param tokenAddress 代币地址
     * @return 代币余额
     */
    public BigInteger getTokenBalanceByAddress(String address, String tokenAddress) {
        BigInteger balance = null;
        try {
            // 构建查询代币余额的方法
            Function function = new Function(
                    "balanceOf",
                    Collections.singletonList(new Address(address)),
                    Collections.singletonList(new TypeReference<Uint256>() {})
            );
            String encodedFunction = FunctionEncoder.encode(function);

            // 发送查询请求
            EthCall response = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(address, tokenAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).sendAsync().get();

            // 解析查询结果
            List<Type> decodedResponse = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            balance = (BigInteger) decodedResponse.get(0).getValue();

            System.out.println("Balance: " + Convert.fromWei(balance.toString(), Convert.Unit.ETHER) + " Tokens");

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("address " + address + "token address " + tokenAddress + " balance " + balance + "wei");
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

        // 凭证
        Credentials credentials = Credentials.create(userWallet.getPrivateKey());

        String txHash = null;
        try {
            //创建RawTransaction交易对象
            Function function = new Function(
                    "transfer",
                    Arrays.asList(new Address(toAddress), new Uint256(value)),
                    List.of(new TypeReference<Type>() { })
            );
            String encodedFunction = FunctionEncoder.encode(function);

            // 构建交易对象
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    getTransactionNonce(fromAddress),
                    getTransactionGasPrice(),
                    DefaultGasProvider.GAS_LIMIT,
                    tokenAddress,
                    value,
                    encodedFunction
            );

            log.info("Token Transaction: {}", JSON.toJSONString(rawTransaction));

            //签名Transaction，这里要对交易做签名
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signMessage);

            //发送交易
            EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

            log.info("Token Transaction Response: {}", JSON.toJSONString(transactionResponse));

            txHash = transactionResponse.getTransactionHash();

            log.info("Token Transaction complete, view it at https://sepolia.etherscan.io/tx/" + txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("tx hash " + txHash);
        return txHash;
    }

    /**
     * 通过交易哈希获取交易详情
     *
     * @param txHash 交易哈希
     * @return 交易详情
     */
    public String getTransactionByHash(String txHash) {
        String tx = null;
        try {
            EthTransaction transaction = web3j.ethGetTransactionByHash(txHash).send();
            tx = JSON.toJSONString(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tx;
    }

    /**
     * 通过交易哈希获取交易结果信息
     *
     * @param txHash 交易哈希
     * @return 交易结果信息
     */
    public TransactionReceipt getTransactionReceiptByHash(String txHash) {
        TransactionReceipt tr = null;
        try {
            EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(txHash).send();

            if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                tr = ethGetTransactionReceipt.getTransactionReceipt().get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tr;
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
                        Transaction tx = ((EthBlock.TransactionObject) txResult).get();

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

    /**
     * 获取当前 Gas 价格
     *
     * @return Gas价格
     */
    private BigInteger getTransactionGasPrice() {
        BigInteger gasPrice = BigInteger.ZERO;
        try {
            EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
            gasPrice = ethGasPrice.getGasPrice();
            System.out.println("Current Gas Price: " + gasPrice.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gasPrice;
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

        // 获取交易次数
//        String txHash = "0x20ae3404898aa5a3e09312d33602e75ca7c5284963fdcb472f654df41b5ffe31";
//        EthTransaction transaction = web3j.ethGetTransactionByHash(txHash).send();
//        System.out.println("EthTransaction is: " + transaction);
//        System.out.println("EthTransaction Object is: " + transaction.getTransaction().get().toString());
//        System.out.println("EthTransaction String is: " + JSON.toJSONString(transaction.getTransaction().get().getNonce()));
//
//        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
//        BigInteger gasPrice = ethGasPrice.getGasPrice();
//
//        BigInteger value = Convert.toWei(String.valueOf(0), Convert.Unit.ETHER).toBigInteger();
//
//        //创建RawTransaction交易对象
//        Function function = new Function(
//                "transfer",
//                Arrays.asList(new Address("0xEEefA9e1F1649c8F197281406636BC83000919B4"), new Uint256(value)),
//                List.of(new TypeReference<Type>() { })
//        );
//        String encodedFunction = FunctionEncoder.encode(function);
//
//        // 构建取消交易对象
//        RawTransaction rawTransaction = RawTransaction.createTransaction(
//                transaction.getTransaction().get().getNonce(),
//                gasPrice.multiply(BigInteger.valueOf(10)),
//                DefaultGasProvider.GAS_LIMIT,
//                "0xbf7521BD6abB6813491c32BfE407E1027A189A64",
//                value,
//                encodedFunction
//        );
//
//        Credentials credentials = Credentials.create("168850a3e0eb88f1b083c04350472d4f0ddf6c69cc1a050912bc89f879820af7");
//
//        //签名Transaction，这里要对交易做签名
//        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
//        String hexValue = Numeric.toHexString(signMessage);
//
//        // 发送交易到网络
//        web3j.ethSendRawTransaction(hexValue)
//                .sendAsync()
//                .thenAccept(transactionReceipt -> {
//                    System.out.println("Transaction Hash: " + transactionReceipt.getTransactionHash());
//                })
//                .exceptionally(throwable -> {
//                    System.err.println("Error: " + throwable.getMessage());
//                    return null;
//                });

    }
}

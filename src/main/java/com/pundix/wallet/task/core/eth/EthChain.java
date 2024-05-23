package com.pundix.wallet.task.core.eth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
import com.pundix.wallet.task.dto.event.TransactionRecordEvent;
import com.pundix.wallet.task.entity.UserWallet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.context.ApplicationEventPublisher;
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
import org.web3j.tx.ChainIdLong;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 以太坊链操作
 */
@Component
@Slf4j
public class EthChain {

    private final Web3j web3j;

    private final ApplicationEventPublisher eventPublisher;

    public EthChain(Web3j web3j, ApplicationEventPublisher eventPublisher) {
        this.web3j = web3j;
        this.eventPublisher = eventPublisher;
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
     * 获取ETH估算Gas
     *
     * @param fromAddress 付款方
     * @param toAddress   收款方
     * @param amount      金额
     * @param nonce       交易次数
     * @param gasPrice    Gas 价格
     * @return 估算Gas
     */
    public BigInteger getETHEstimateGas(String fromAddress, String toAddress, BigDecimal amount, BigInteger nonce, BigInteger gasPrice) {
        BigInteger estimateGasLimit = BigInteger.ZERO;
        try {
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            // 创建交易对象
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                    fromAddress,
                    nonce,
                    gasPrice,
                    BigInteger.ZERO,
                    toAddress,
                    value
            );

            // 估算Gas Limit
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();
            estimateGasLimit = ethEstimateGas.getAmountUsed();
            log.info("Estimated Gas limit: " + estimateGasLimit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return estimateGasLimit;
    }

    /**
     * 发送ETH交易(指定Gas)
     *
     * @param fromAddress 付款方
     * @param toAddress   收款方
     * @param amount      金额
     * @param gasPrice    Gas 价格
     * @param gasLimit    Gas 限制
     * @param nonce       交易次数
     * @return 交易 Hash
     */
    public String sendETHTransaction(String fromAddress, String toAddress, BigDecimal amount, BigInteger gasPrice, BigInteger gasLimit, BigInteger nonce) {
        String txHash = null;
        try {
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            // 获取凭证
            Credentials credentials = Credentials.create(fromAddress);

            // 创建交易对象
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value
            );

            // 对交易签名
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, ChainIdLong.MAINNET, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // 发送交易并广播
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            txHash = ethSendTransaction.getTransactionHash();

            log.info("ETH Transaction Response: {}", ethSendTransaction);
            log.info("ETH Transaction complete, view it at https://sepolia.etherscan.io/tx/" + txHash);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return txHash;
    }

    /**
     * 直接发送ETH交易（不关心Gas）
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
     * 获取Token估算Gas
     *
     * @param fromAddress 付款方
     * @param toAddress   收款方
     * @param amount      金额
     * @param tokenAddress 代币地址
     * @return 估算Gas
     */
    public BigInteger getTokenEstimateGas(String fromAddress, String toAddress, BigDecimal amount, String tokenAddress) {
        BigInteger estimateGasLimit = BigInteger.ZERO;
        try {
            BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();

            // 创建transfer方法
            String encodedFunction = createTransferFunction(toAddress, value);

            // 创建交易对象
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    fromAddress,
                    tokenAddress,
                    encodedFunction
            );
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();

            // 估算Gas Limit
            estimateGasLimit = ethEstimateGas.getAmountUsed();
            log.info("Estimated Gas limit: " + estimateGasLimit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return estimateGasLimit;
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
            // 构建transfer方法
            String encodedFunction = createTransferFunction(toAddress, value);

            // 构建交易对象
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    getTransactionNonce(fromAddress),
                    getTransactionGasPrice(),
                    DefaultGasProvider.GAS_LIMIT,
                    tokenAddress,
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
     * 创建转账方法
     *
     * @param toAddress 收款地址
     * @param value     金额
     * @return 转账方法
     */
    private String createTransferFunction(String toAddress, BigInteger value) {
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(toAddress), new Uint256(value)),
                List.of(new TypeReference<Type>() { })
        );
        return FunctionEncoder.encode(function);
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
    public BigInteger getTransactionNonce(String address) {
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
    public BigInteger getTransactionGasPrice() {
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

    /**
     * 监听交易
     *
     * @param addresses 需要监听的地址
     */
    public void listenTransactions(List<String> addresses) {
        log.info("开始监听链上交易");
        web3j.transactionFlowable().subscribe(tx -> {
            // 异步处理交易记录事件
            CompletableFuture.runAsync(() -> {
                asyncHandleTransactionRecordEvent(addresses, tx);
            });
        }, Throwable::printStackTrace);
    }

    private void asyncHandleTransactionRecordEvent(List<String> addresses, Transaction transaction) {
        // 判断是否是需要监听的地址(只处理我们地址相关的交易)
        if (addresses.contains(transaction.getFrom()) || addresses.contains(transaction.getTo())) {
            // 发送内部事件（解耦，避免与Service服务互相调用）
            TransactionRecordEvent transactionRecordEvent = new TransactionRecordEvent(this, transaction);
            eventPublisher.publishEvent(transactionRecordEvent);

            log.info("交易地址符合，发送交易事件: {}", transactionRecordEvent);
        }
    }

    /**
     * 监听Block交易
     *
     * @param addresses 需要监听的地址
     */
    public void listenBlocks(List<String> addresses) {
        log.info("开始监听Block交易");
        // 监听链上新Block上所有交易
        web3j.blockFlowable(false).subscribe(block -> {
            EthBlock.Block currentBlock = block.getBlock();
            log.info("当前Block: {}", currentBlock.getNumber().toString());
            List<EthBlock.TransactionResult> transactions = currentBlock.getTransactions();
            transactions.forEach(txResult -> {
                // 异步处理交易记录事件
                CompletableFuture.runAsync(() -> {
                    asyncHandleBlockTransactionRecordEvent(addresses, txResult);
                });
            });
        }, Throwable::printStackTrace);
    }

    private void asyncHandleBlockTransactionRecordEvent(List<String> addresses, EthBlock.TransactionResult txResult) {
        Transaction transaction = null;
        if (txResult instanceof EthBlock.TransactionObject) {
            EthBlock.TransactionObject transactionObject = (EthBlock.TransactionObject) txResult;
            transaction = transactionObject.get();
        } else if (txResult instanceof EthBlock.TransactionHash) {
            String transactionHash = (String) txResult.get();
            try {
                EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
                transaction = ethTransaction.getTransaction().orElse(null);
            } catch (Exception e) {
                return;
            }
        } else {
            log.info("当前交易不是标准交易: {}", txResult);
        }

        if (transaction != null) {
            // 判断是否是需要监听的地址(只处理我们地址相关的交易)
            if (addresses.contains(transaction.getFrom()) || addresses.contains(transaction.getTo())) {
                // 发送内部事件（解耦，避免与Service服务互相调用）
                TransactionRecordEvent transactionRecordEvent = new TransactionRecordEvent(this, transaction);
                eventPublisher.publishEvent(transactionRecordEvent);

                log.info("交易地址符合，发送交易事件: {}", transactionRecordEvent);
            }
        }
    }

    /**
     * 解析输入参数
     *
     * @param input 输入参数
     * @return 解析后的参数
     */
    public String serializeInput(String input) {
        JSONObject jsonObject = new JSONObject();
        Function transferFunction = new Function(
                "transfer",
                Arrays.asList(new Address("0x0"), new Uint256(0)),  // 参数列表
                Arrays.asList()  // 输出参数列表
        );

        if (input.startsWith(FunctionEncoder.encode(transferFunction).substring(0, 10))) {
            // 去掉方法签名的前缀
            String encodedParams = input.substring(10);
            List<TypeReference<Type>> parameterTypes = new ArrayList<>();
            for (Type type : transferFunction.getInputParameters()) {
                // 获取 Type 的类对象
                Class<? extends Type> typeClass = type.getClass();
                // 使用 TypeReference 的构造函数创建 TypeReference 对象
                TypeReference<Type> typeReference = (TypeReference<Type>) TypeReference.create(typeClass);
                parameterTypes.add(typeReference);
            }
            List<Type> parameters = FunctionReturnDecoder.decode(encodedParams, parameterTypes);

            String to = (String) parameters.get(0).getValue();
            BigInteger value = ((Uint256) parameters.get(1)).getValue();

            System.out.println("Transfer method=transfer, to=" + to + ", value=" + value);

            jsonObject.put("functionName", "transfer");
            jsonObject.put("to", to);
            jsonObject.put("value", value);
            return jsonObject.toJSONString();
        }
        return null;
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

//        String input = "0xa9059cbb000000000000000000000000eeefa9e1f1649c8f197281406636bc83000919b40000000000000000000000000000000000000000000000000e043da617250000";
//        Function transferFunction = new Function(
//                "transfer",
//                Arrays.asList(new Address("0x0"), new Uint256(0)),  // 参数列表
//                Arrays.asList()  // 输出参数列表
//        );
//
//        String encode = FunctionEncoder.encode(transferFunction);
//
//        System.out.println("Transfer method: to=" + encode);
//
//        if (input.startsWith(encode.substring(0, 10))) {
//            String encodedParams = input.substring(10); // 去掉方法签名的前缀
//
//            List<TypeReference<Type>> typeReferences = new ArrayList<>();
//            for (Type type : transferFunction.getInputParameters()) {
//                // 获取 Type 的类对象
//                Class<? extends Type> typeClass = type.getClass();
//                // 使用 TypeReference 的构造函数创建 TypeReference 对象
//                TypeReference<Type> typeReference = (TypeReference<Type>) TypeReference.create(typeClass);
//                typeReferences.add(typeReference);
//            }
//            List<Type> parameters = FunctionReturnDecoder.decode(encodedParams, typeReferences);
//
//            String to = (String) parameters.get(0).getValue();
//            BigInteger value = ((Uint256) parameters.get(1)).getValue();
//
//            System.out.println("Transfer method: to=" + to + ", value=" + value);
//        }

    }
}

package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.entity.GatherGasRecord;
import com.pundix.wallet.task.entity.GatherUSDTRecord;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.enums.ETHCoinTypeEnum;
import com.pundix.wallet.task.enums.TransactionStatusEnum;
import com.pundix.wallet.task.repository.GatherGasRecordRepository;
import com.pundix.wallet.task.repository.GatherUSDTRecordRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ETH USDT归集服务
 */
@Service
@Slf4j
public class GatherUSDTService {

    private final UserWalletRepository userWalletRepository;

    private final GatherGasRecordRepository gatherGasRecordRepository;

    private final GatherUSDTRecordRepository gatherUSDTRecordRepository;

    private final EthChain ethChain;

    public GatherUSDTService(UserWalletRepository userWalletRepository, GatherGasRecordRepository gatherGasRecordRepository, GatherUSDTRecordRepository gatherUSDTRecordRepository, EthChain ethChain) {
        this.userWalletRepository = userWalletRepository;
        this.gatherGasRecordRepository = gatherGasRecordRepository;
        this.gatherUSDTRecordRepository = gatherUSDTRecordRepository;
        this.ethChain = ethChain;
    }

    // USDT合约地址
    private final String USDT_TOKEN_ADDRESS = ETHCoinTypeEnum.USDT.getTokenAddress();

    /**
     * 定时任务：每天凌晨1点执行
     *      TODO 暂时关闭，开启增加注解 @Scheduled(cron = "0 0 1 * * ?")
     * 归集所有用户的USDT
     */
    @Async
    public void gatherUSDT() {
        // 归集钱包
        UserWallet gatherWallet = getGatherWallet();

        // 查询所有用户的ETH钱包地址
        List<UserWallet> userWallets = getAllUserWallets();
        // 排除掉归集地址
        userWallets = userWallets.stream().filter(userWallet -> !userWallet.getAddress().equals(gatherWallet.getAddress())).collect(Collectors.toList());

        // 遍历所有用户的钱包地址
        userWallets.forEach(userWallet -> {
            // 查询用户USDT的余额
            BigInteger balance = ethChain.getTokenBalanceByAddress(userWallet.getAddress(), USDT_TOKEN_ADDRESS);
            BigDecimal balanceInEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);

            // 如果余额大于10
            if (balanceInEther.compareTo(new BigDecimal("10")) > 0) {
                // 发送0.1到归集地址
                BigDecimal gasAmountEther = new BigDecimal("0.1");

                // 向用户钱包发送0.1USDT，作为Gas费用
                String txHash = ethChain.sendTokenTransaction(gatherWallet, userWallet.getAddress(), gasAmountEther, USDT_TOKEN_ADDRESS);

                // 保存发送Gas记录
                saveGatherGasRecord(userWallet, txHash, balanceInEther);
            }
        });
    }

    /**
     * 定时任务：上个任务执行完成后5分钟后执行
     * 处理所有Pending的发送Gas的交易
     * 更新发送Gas交易状态
     * 发起USDT归集交易
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void handleGasRecords() {
        // 获取所有Pending的交易
        List<GatherGasRecord> pendingRecords = gatherGasRecordRepository.findPendingRecords();
        if (pendingRecords == null || pendingRecords.isEmpty()) {
            log.info("没有待处理的发送Gas交易");
            return;
        }

        // 归集地址
        String gatherAddress = getGatherAddress();

        // 遍历检查所有交易的状态
        pendingRecords.forEach(
            gatherGasRecord -> {
                // 检查发送Gas的交易状态
                TransactionReceipt receipt = ethChain.getTransactionReceiptByHash(gatherGasRecord.getTxHash());
                if (receipt.isStatusOK()) {
                    // 更新发送Gas交易状态
                    gatherGasRecord.setStatus(TransactionStatusEnum.SUCCESS);
                    gatherGasRecord.setUpdateTime(new Date());
                    gatherGasRecordRepository.save(gatherGasRecord);

                    userWalletRepository.findById(gatherGasRecord.getUserWalletId()).ifPresent(userWallet -> {
                        // 查询用户USDT的余额
                        BigInteger balance = ethChain.getTokenBalanceByAddress(userWallet.getAddress(), USDT_TOKEN_ADDRESS);
                        BigDecimal balanceInEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);

                        // 用户USDT余额大于10
                        if (balanceInEther.compareTo(new BigDecimal("10")) > 0) {
                            // 将用户的USDT余额减去发送的Gas费用
                            BigDecimal gatherAmountEther = balanceInEther.subtract(gatherGasRecord.getGasAmountEther());

                            // 向归集地址发送用户的USDT余额
                            String txHash = ethChain.sendTokenTransaction(userWallet, gatherAddress, gatherAmountEther, USDT_TOKEN_ADDRESS);

                            // 保存发送USDT记录
                            saveGatherUSDTRecord(userWallet, txHash, balance, balanceInEther);
                        }
                    });

                    log.info("Transaction Successful, ETH record id: {}", gatherGasRecord.getId());
                } else {
                    // 更新交易状态
                    gatherGasRecord.setStatus(TransactionStatusEnum.SUCCESS);
                    gatherGasRecord.setUpdateTime(new Date());
                    gatherGasRecordRepository.save(gatherGasRecord);

                    log.error("Transaction Failed, ETH record id: {}", gatherGasRecord.getId());
                }
            }
        );
    }

    /**
     * 定时任务：上个任务执行完成后5分钟后执行
     * 处理所有Pending的归集USDT的交易
     * 更新交易状态
     * 更新用户钱包余额
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void handleUSDTRecords() {
        // 获取所有Pending的交易
        List<GatherUSDTRecord> pendingRecords = gatherUSDTRecordRepository.findPendingRecords();
        if (pendingRecords == null || pendingRecords.isEmpty()) {
            log.info("没有待处理的USDT归集交易");
            return;
        }
        // 遍历检查所有交易的状态
        pendingRecords.forEach(
                gatherUSDTRecord -> {
                    // 检查归集USDT交易状态
                    TransactionReceipt receipt = ethChain.getTransactionReceiptByHash(gatherUSDTRecord.getTxHash());
                    if (receipt.isStatusOK()) {
                        // 更新归集USDT交易状态
                        gatherUSDTRecord.setStatus(TransactionStatusEnum.SUCCESS);
                        gatherUSDTRecord.setUpdateTime(new Date());
                        gatherUSDTRecordRepository.save(gatherUSDTRecord);

                        // 更新用户钱包余额
                        userWalletRepository.findById(gatherUSDTRecord.getUserWalletId())
                                .ifPresent(userWallet -> {
                                    userWallet.setGatherAmountWei(userWallet.getGatherAmountWei().add(gatherUSDTRecord.getGatherAmountWei()));
                                    userWallet.setGatherAmountEther(userWallet.getGatherAmountEther().add(gatherUSDTRecord.getGatherAmountEther()));
                                    userWallet.setUpdateTime(new Date());
                                    userWalletRepository.save(userWallet);
                                });

                        log.info("Transaction Successful, USDT record id: {}", gatherUSDTRecord.getId());
                    } else {
                        // 更新归集USDT交易状态
                        gatherUSDTRecord.setStatus(TransactionStatusEnum.SUCCESS);
                        gatherUSDTRecord.setUpdateTime(new Date());
                        gatherUSDTRecordRepository.save(gatherUSDTRecord);

                        log.error("Transaction Failed, USDT record id: {}", gatherUSDTRecord.getId());
                    }
                }
        );
    }

    private List<UserWallet> getAllUserWallets() {
        return userWalletRepository.findAll();
    }

    private UserWallet getGatherWallet() {
        return userWalletRepository.findById(0)
                .orElseThrow(() -> new RuntimeException("未找到归集地址"));
    }

    private String getGatherAddress() {
        return userWalletRepository.findById(0)
                .orElseThrow(() -> new RuntimeException("未找到归集地址"))
                .getAddress();
    }

    private void saveGatherGasRecord(UserWallet userWallet, String txHash, BigDecimal gasAmountEther) {
        GatherGasRecord gatherGasRecord = new GatherGasRecord();
        gatherGasRecord.setUserId(userWallet.getUserId());
        gatherGasRecord.setUserWalletId(userWallet.getId());
        gatherGasRecord.setTxHash(txHash);
        gatherGasRecord.setStatus(TransactionStatusEnum.PENDING);
        gatherGasRecord.setGasAmountWei(Convert.toWei(gasAmountEther, Convert.Unit.ETHER).toBigInteger());
        gatherGasRecord.setGasAmountEther(gasAmountEther);
        gatherGasRecord.setCreateTime(new Date());
        gatherGasRecord.setUpdateTime(new Date());
        gatherGasRecordRepository.save(gatherGasRecord);
    }

    private void saveGatherUSDTRecord(UserWallet userWallet, String txHash, BigInteger gatherAmount, BigDecimal gatherAmountEther) {
        GatherUSDTRecord gatherUSDTRecord = new GatherUSDTRecord();
        gatherUSDTRecord.setUserId(userWallet.getUserId());
        gatherUSDTRecord.setUserWalletId(userWallet.getId());
        gatherUSDTRecord.setTxHash(txHash);
        gatherUSDTRecord.setStatus(TransactionStatusEnum.PENDING);
        gatherUSDTRecord.setUserAmountWei(gatherAmount);
        gatherUSDTRecord.setUserAmountEther(gatherAmountEther);
        gatherUSDTRecord.setGatherAmountWei(gatherAmount);
        gatherUSDTRecord.setGatherAmountEther(gatherAmountEther);
        gatherUSDTRecord.setCreateTime(new Date());
        gatherUSDTRecord.setUpdateTime(new Date());
        gatherUSDTRecordRepository.save(gatherUSDTRecord);
    }

}

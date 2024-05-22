package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.entity.GatherETHRecord;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.enums.TransactionStatusEnum;
import com.pundix.wallet.task.repository.GatherETHRecordRepository;
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
 * ETH 主币归集服务
 */
@Service
@Slf4j
public class GatherETHService {

    private final UserWalletRepository userWalletRepository;

    private final GatherETHRecordRepository gatherETHRecordRepository;

    private final EthChain ethChain;

    public GatherETHService(UserWalletRepository userWalletRepository, GatherETHRecordRepository gatherETHRecordRepository, EthChain ethChain) {
        this.userWalletRepository = userWalletRepository;
        this.gatherETHRecordRepository = gatherETHRecordRepository;
        this.ethChain = ethChain;
    }

    /**
     * 定时任务：每天凌晨0点执行
     *      TODO 暂时关闭，开启增加注解 @Scheduled(cron = "0 0 0 * * ?")
     * 归集所有用户的ETH
     */
    @Async
    public void gatherETH() {
        // 归集地址
        String gatherAddress = getGatherAddress();

        // 查询所有用户的ETH钱包地址
        List<UserWallet> userWallets = getAllUserWallets();
        // 排除掉归集地址
        userWallets = userWallets.stream().filter(userWallet -> !userWallet.getAddress().equals(gatherAddress)).collect(Collectors.toList());

        // 遍历所有用户的ETH钱包地址
        userWallets.forEach(userWallet -> {
            // 查询用户ETH钱包地址的余额
            BigInteger balance = ethChain.getBalanceByAddress(userWallet.getAddress());
            BigDecimal balanceInEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);

            BigInteger nonce = ethChain.getTransactionNonce(userWallet.getAddress());
            // 获取Gas   大于Gas费用的就归集
            BigInteger gasPrice = ethChain.getTransactionGasPrice();
            // 获取估算Gas Limit
            BigInteger gasLimit = ethChain.getETHEstimateGas(userWallet.getAddress(), gatherAddress, balanceInEther, nonce, gasPrice);
            // 计算Gas总费用
            BigInteger totalGasCost = gasPrice.multiply(gasLimit);

            // 如果余额大于Gas总费用
            if (balance.compareTo(totalGasCost) > 0) {
                // 计算实际归集金额
                BigInteger amountToGather = balance.subtract(totalGasCost);
                BigDecimal amountToGatherInEther = Convert.fromWei(amountToGather.toString(), Convert.Unit.ETHER);

                // 再次估算Gas费用(因实际归集金额不同，Gas费用不同)
                gasLimit = ethChain.getETHEstimateGas(userWallet.getAddress(), gatherAddress, amountToGatherInEther, nonce, gasPrice);
                totalGasCost = gasPrice.multiply(gasLimit);
                // 最终实际归集金额
                amountToGather = balance.subtract(totalGasCost);

                if (amountToGather.compareTo(BigInteger.ZERO) > 0) {
                    amountToGatherInEther = Convert.fromWei(amountToGather.toString(), Convert.Unit.ETHER);

                    // 则将ETH归集到指定地址
                    String txHash = ethChain.sendETHTransaction(userWallet.getAddress(), gatherAddress, amountToGatherInEther, gasPrice, gasLimit, nonce);

                    // 保存归集交易记录
                    saveGatherETHRecord(userWallet, txHash, balance, balanceInEther, amountToGatherInEther);
                }
            }
        });
    }

    /**
     * 定时任务：上个任务执行完成后5分钟后执行
     * 处理所有Pending的交易
     * 更新交易状态
     * 更新用户钱包余额
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void handleBalance() {
        // 获取所有Pending的交易
        List<GatherETHRecord> pendingRecords = gatherETHRecordRepository.findPendingRecords();
        if (pendingRecords == null || pendingRecords.isEmpty()) {
            log.info("没有待处理的归集ETH交易");
            return;
        }
        // 遍历检查所有交易的状态
        pendingRecords.forEach(
            gatherETHRecord -> {
                // 检查交易状态
                TransactionReceipt receipt = ethChain.getTransactionReceiptByHash(gatherETHRecord.getTxHash());
                if (receipt.isStatusOK()) {
                    // 更新交易状态
                    gatherETHRecord.setStatus(TransactionStatusEnum.SUCCESS);
                    gatherETHRecord.setUpdateTime(new Date());
                    gatherETHRecordRepository.save(gatherETHRecord);

                    // 更新用户钱包归集ETH金额
                    userWalletRepository.findById(gatherETHRecord.getUserWalletId())
                        .ifPresent(userWallet -> {
                            userWallet.setGatherETHAmountWei(userWallet.getGatherETHAmountWei().add(gatherETHRecord.getGatherAmountWei()));
                            userWallet.setGatherETHAmountEther(userWallet.getGatherETHAmountEther().add(gatherETHRecord.getGatherAmountEther()));
                            userWallet.setUpdateTime(new Date());
                            userWalletRepository.save(userWallet);
                        });

                    log.info("Transaction Successful, ETH record id: {}", gatherETHRecord.getId());
                } else {
                    // 更新交易状态
                    gatherETHRecord.setStatus(TransactionStatusEnum.FAIL);
                    gatherETHRecord.setUpdateTime(new Date());
                    gatherETHRecordRepository.save(gatherETHRecord);

                    // 重试  告警

                    log.error("Transaction Failed, ETH record id: {}", gatherETHRecord.getId());
                }
            }
        );
    }


    private List<UserWallet> getAllUserWallets() {
        return userWalletRepository.findAll();
    }

    private String getGatherAddress() {
        return userWalletRepository.findById(0)
                .orElseThrow(() -> new RuntimeException("未找到归集地址"))
                .getAddress();
    }

    private void saveGatherETHRecord(UserWallet userWallet, String txHash, BigInteger balance, BigDecimal balanceInEther, BigDecimal gatherAmountEther) {
        GatherETHRecord gatherETHRecord = new GatherETHRecord();
        gatherETHRecord.setUserId(userWallet.getUserId());
        gatherETHRecord.setUserWalletId(userWallet.getId());
        gatherETHRecord.setTxHash(txHash);
        gatherETHRecord.setStatus(TransactionStatusEnum.PENDING);
        gatherETHRecord.setUserAmountWei(balance);
        gatherETHRecord.setUserAmountEther(balanceInEther);
        gatherETHRecord.setGatherAmountWei(Convert.toWei(gatherAmountEther, Convert.Unit.ETHER).toBigInteger());
        gatherETHRecord.setGatherAmountEther(gatherAmountEther);
        gatherETHRecord.setCreateTime(new Date());
        gatherETHRecord.setUpdateTime(new Date());
        gatherETHRecordRepository.save(gatherETHRecord);
    }

}

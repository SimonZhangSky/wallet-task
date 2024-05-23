package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
import com.pundix.wallet.task.dto.event.TransactionRecordEvent;
import com.pundix.wallet.task.entity.TransactionRecord;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.enums.TransactionStatusEnum;
import com.pundix.wallet.task.repository.TransactionRecordRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易记录服务
 */
@Service
@Slf4j
public class TransactionRecordService {

    private final UserWalletRepository userWalletRepository;

    private final TransactionRecordRepository transactionRecordRepository;

    private final EthChain ethChain;

    public TransactionRecordService(UserWalletRepository userWalletRepository, TransactionRecordRepository transactionRecordRepository, EthChain ethChain) {
        this.userWalletRepository = userWalletRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.ethChain = ethChain;
    }

    /*
     地址 钱包Map key:地址 value:钱包, 用于快速查找用户钱包

     最好在Redis中储存，并且在用户钱包发生变化时更新Redis
     */
    private Map<String, UserWallet> userWalletMap;

    @PostConstruct
    public void startListen() {
        // 获取所有地址
        List<UserWallet> userWallets = userWalletRepository.findAll();

        // 初始化地址 钱包Map
        userWalletMap = userWallets.stream().collect(Collectors.toMap(UserWallet::getAddress, userWallet -> userWallet));

        log.info("初始化钱包信息成功：{}", userWalletMap);

        // 需要监听用户的所有地址
        List<String> addresses = new ArrayList<>(userWalletMap.keySet());

        // 开启监听交易记录
        ethChain.listenTransactions(addresses);
    }

    /**
     * 交易记录事件处理(保存交易记录)
     *
     * @param event 交易记录事件
     */
    @Async
    @EventListener
    public void handleTransactionRecordEvent(TransactionRecordEvent event) {
        log.info("TransactionRecordEvent: {}", event);
        Transaction transaction = event.getTransaction();

        String from = transaction.getFrom();
        String to = transaction.getTo();

        UserWallet fromUserWallet = userWalletMap.get(from);
        UserWallet toUserWallet = userWalletMap.get(to);

        TransactionRecord transactionRecord = new TransactionRecord();
        if (fromUserWallet != null) {
            transactionRecord.setFromUserId(fromUserWallet.getUserId());
            transactionRecord.setFromUserWalletId(fromUserWallet.getId());
        }
        transactionRecord.setFromAddress(from);
        if (toUserWallet != null) {
            transactionRecord.setToUserId(toUserWallet.getUserId());
            transactionRecord.setToUserWalletId(toUserWallet.getId());
        }
        transactionRecord.setToAddress(to);
        transactionRecord.setTxHash(transaction.getHash());
        transactionRecord.setBlockNumber(transaction.getBlockNumber());
        transactionRecord.setNonce(transaction.getNonce());
        transactionRecord.setGasPrice(transaction.getGasPrice());
        transactionRecord.setGasLimit(transaction.getGas());
        transactionRecord.setGasUsed(transaction.getGas());
        transactionRecord.setValue(transaction.getValue());
        transactionRecord.setStatus(TransactionStatusEnum.PENDING);
        String input = transaction.getInput();
        transactionRecord.setInput(transaction.getInput());
        // 合约交易
        if (input != null && !"0x".equals(input)) {
            // 解析合约交易方法(目前只解析transfer方法)
            String functionInfo = ethChain.serializeInput(input);
            transactionRecord.setFunctionInfo(functionInfo);
        }
        transactionRecord.setCreates(transaction.getCreates());
        transactionRecord.setPublicKey(transaction.getPublicKey());
        transactionRecord.setCreateTime(new Date());
        transactionRecord.setUpdateTime(new Date());
        transactionRecordRepository.save(transactionRecord);

        log.info("Transaction record saved: {}", transactionRecord);
    }


    /**
     * 定时任务：上个任务执行完成后30秒后执行
     * 处理所有Pending的交易
     * 更新交易状态
     * 更新用户钱包余额
     */
    @Scheduled(fixedDelay = 30 * 1000L)
    public void handleBalance() {
        // 获取所有Pending的交易
        List<TransactionRecord> pendingRecords = transactionRecordRepository.findPendingRecords();
        if (pendingRecords == null || pendingRecords.isEmpty()) {
            log.info("没有待处理交易记录");
            return;
        }
        // 遍历检查所有交易的状态
        pendingRecords.forEach(
            transactionRecord -> {
                // 检查交易状态
                TransactionReceipt receipt = ethChain.getTransactionReceiptByHash(transactionRecord.getTxHash());
                if (receipt.isStatusOK()) {
                    // 更新交易状态(成功)
                    transactionRecord.setGasUsed(receipt.getGasUsed());
                    transactionRecord.setStatus(TransactionStatusEnum.SUCCESS);

                    log.info("Transaction record update to Successful, transaction record id: {}", transactionRecord.getId());
                } else {
                    // 更新交易状态(失败)
                    transactionRecord.setStatus(TransactionStatusEnum.FAIL);

                    // 重试  告警

                    log.info("Transaction record update to Failed, transaction record id: {}", transactionRecord.getId());
                }
                transactionRecord.setUpdateTime(new Date());
                transactionRecordRepository.save(transactionRecord);
            }
        );
    }

    /**
     * 交易记录列表
     *
     * @param userId 用户ID
     * @param transactionListRequest 交易记录列表请求
     * @return 交易记录分页
     */
    public Page<TransactionRecord> transactionList(int userId, UserTransactionListRequest transactionListRequest) {
        UserWallet userWallet = userWalletRepository.findByUserIdAndAddress(userId, transactionListRequest.getFromAddress());
        if (userWallet == null) {
            throw new EntityNotFoundException("UserWallet not found");
        }
        int page = transactionListRequest.getPage();
        int pageSize = transactionListRequest.getPageSize();
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<TransactionRecord> transactionRecords = transactionRecordRepository.findByFilters(userWallet.getAddress(), transactionListRequest.getStartBlock(), transactionListRequest.getEndBlock(), pageable);

        return transactionRecords;
    }

}

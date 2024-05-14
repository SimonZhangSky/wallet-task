package com.pundix.wallet.task.service;

import com.github.pagehelper.PageInfo;
import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.dto.UserTradingSendETHRequest;
import com.pundix.wallet.task.dto.UserTradingSendTokenRequest;
import com.pundix.wallet.task.dto.UserTransactionListRequest;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.entity.UserTrading;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.repository.UserRepository;
import com.pundix.wallet.task.repository.UserTradingRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;

import javax.persistence.EntityNotFoundException;
import java.util.Date;

@Service
@Slf4j
public class UserTradingService {

    private final UserRepository userRepository;

    private final UserWalletRepository userWalletRepository;

    private final UserTradingRepository userTradingRepository;

    private final EthChain ethChain;

    public UserTradingService(UserRepository userRepository, UserWalletRepository userWalletRepository, UserTradingRepository userTradingRepository, EthChain ethChain) {
        this.userRepository = userRepository;
        this.userWalletRepository = userWalletRepository;
        this.userTradingRepository = userTradingRepository;
        this.ethChain = ethChain;
    }

    public String sendETH(int userId, UserTradingSendETHRequest sendETHRequest) {
        UserWallet userWallet = getUserWallet(userId, sendETHRequest.getUserWalletId());

        String txHash = ethChain.sendETHTransaction(userWallet, sendETHRequest.getToAddress(), sendETHRequest.getAmount());

        UserTrading userTrading = new UserTrading();
        userTrading.setUserId(userId);
        userTrading.setUserWalletId(userWallet.getId());
        userTrading.setTxHash(txHash);
        userTrading.setCreateTime(new Date());
        userTradingRepository.save(userTrading);

        return txHash;
    }

    public String sendToken(int userId, UserTradingSendTokenRequest sendTokenRequest) {
        UserWallet userWallet = getUserWallet(userId, sendTokenRequest.getUserWalletId());

        String txHash = ethChain.sendTokenTransaction(userWallet, sendTokenRequest.getToAddress(), sendTokenRequest.getAmount(), sendTokenRequest.getTokenAddress());

        UserTrading userTrading = new UserTrading();
        userTrading.setUserId(userId);
        userTrading.setUserWalletId(userWallet.getId());
        userTrading.setTxHash(txHash);
        userTrading.setCreateTime(new Date());
        userTradingRepository.save(userTrading);

        return txHash;
    }

    public PageInfo<Transaction> transactionList(int userId, UserTransactionListRequest transactionListRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        UserWallet userWallet = userWalletRepository.findByUserIdAndAddress(user.getId(), transactionListRequest.getFromAddress());
        if (userWallet == null) {
            throw new EntityNotFoundException("UserWallet not found");
        }
        if (transactionListRequest.getCoinType() == null) {
            throw new EntityNotFoundException("CoinType not found");
        }

        return ethChain.getTransactionList(transactionListRequest);
    }

    @NotNull
    private UserWallet getUserWallet(int userId, int sendETHRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserWallet userWallet = userWalletRepository.findById(sendETHRequest)
                .orElseThrow(() -> new EntityNotFoundException("UserWallet not found"));

        if (userWallet.getUserId() != user.getId()) {
            throw new IllegalArgumentException("UserWallet not belong to user");
        }
        return userWallet;
    }
}

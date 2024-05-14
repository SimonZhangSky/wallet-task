package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.dto.UserTradingSendETHRequest;
import com.pundix.wallet.task.dto.UserTradingSendTokenRequest;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.repository.UserRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@Slf4j
public class UserTradingService {

    private final UserRepository userRepository;

    private final UserWalletRepository userWalletRepository;

    private final EthChain ethChain;

    public UserTradingService(UserRepository userRepository, UserWalletRepository userWalletRepository, EthChain ethChain) {
        this.userRepository = userRepository;
        this.userWalletRepository = userWalletRepository;
        this.ethChain = ethChain;
    }

    public String sendETH(int userId, UserTradingSendETHRequest sendETHRequest) {
        UserWallet userWallet = getUserWallet(userId, sendETHRequest.getUserWalletId());

        String txHash = ethChain.sendETHTransaction(userWallet, sendETHRequest.getToAddress(), sendETHRequest.getAmount());

        return txHash;
    }

    public String sendToken(int userId, UserTradingSendTokenRequest sendTokenRequest) {
        UserWallet userWallet = getUserWallet(userId, sendTokenRequest.getUserWalletId());

        String txHash = ethChain.sendTokenTransaction(userWallet, sendTokenRequest.getToAddress(), sendTokenRequest.getAmount(), sendTokenRequest.getTokenAddress());

        return txHash;
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

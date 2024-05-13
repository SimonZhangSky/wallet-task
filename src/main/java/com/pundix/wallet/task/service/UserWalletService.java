package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.dto.UserWalletResponse;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.enums.WalletStatusEnum;
import com.pundix.wallet.task.enums.WalletTpyeEnum;
import com.pundix.wallet.task.repository.UserRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;
import org.web3j.utils.Convert;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserWalletService {

    private final UserRepository userRepository;

    private final UserWalletRepository userWalletRepository;

    private final EthChain ethChain;

    public UserWalletService(UserRepository userRepository, UserWalletRepository userWalletRepository, EthChain ethChain) {
        this.userRepository = userRepository;
        this.userWalletRepository = userWalletRepository;
        this.ethChain = ethChain;
    }

    public List<UserWalletResponse> getAllUserWallets(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<UserWallet> userWallets = userWalletRepository.findByUserId(user.getId());

        return userWallets.stream()
                .map(userWallet -> {
                    BigInteger balance = ethChain.getBalanceByAddress(userWallet.getAddress());
                    BigDecimal balanceInEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);
                    return new UserWalletResponse(
                            userWallet.getWalletType(),
                            userWallet.getAddress(),
                            userWallet.getPrivateKey(),
                            userWallet.getPublicKey(),
                            userWallet.getStatus(),
                            userWallet.getRemark(),
                            userWallet.isDefault(),
                            balance,
                            balanceInEther
                    );
                })
                .collect(Collectors.toList());
    }

    public UserWalletResponse createWallet(int userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        Triple<String, BigInteger, BigInteger> triple = ethChain.generateAddress();

        log.info("Create wallet for user: {}, address: {}, privateKey: {}, publicKey: {}", user.getId(), triple.getLeft(), triple.getMiddle(), triple.getRight());

        UserWallet userWallet = getUserWallet(triple, user);
        userWalletRepository.save(userWallet);

        return new UserWalletResponse(
            userWallet.getWalletType(),
            userWallet.getAddress(),
            userWallet.getPrivateKey(),
            userWallet.getPublicKey(),
            userWallet.getStatus(),
            userWallet.getRemark(),
            userWallet.isDefault(),
            new BigInteger("0"),
            new BigDecimal("0")
        );
    }

    private UserWallet getUserWallet(Triple<String, BigInteger, BigInteger> triple, User user) {
        String address = triple.getLeft();
        BigInteger privateKey = triple.getMiddle();
        BigInteger publicKey = triple.getRight();

        UserWallet userWallet = new UserWallet();
        userWallet.setUserId(user.getId());
        userWallet.setWalletType(WalletTpyeEnum.ETH);
        userWallet.setAddress(address);
        userWallet.setPrivateKey(privateKey.toString());
        userWallet.setPublicKey(publicKey.toString());
        userWallet.setStatus(WalletStatusEnum.NORMAL);
        userWallet.setDefault(false);
        userWallet.setCreateTime(new Date());
        userWallet.setUpdateTime(new Date());
        return userWallet;
    }
}

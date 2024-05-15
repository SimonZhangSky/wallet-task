package com.pundix.wallet.task.service;

import com.pundix.wallet.task.core.eth.EthChain;
import com.pundix.wallet.task.dto.UserWalletResponse;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.entity.UserWallet;
import com.pundix.wallet.task.enums.WalletTpyeEnum;
import com.pundix.wallet.task.repository.UserRepository;
import com.pundix.wallet.task.repository.UserWalletRepository;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserWalletServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private EthChain ethChain;

    @InjectMocks
    private UserWalletService userWalletService;

    @Test
    public void testGetAllUserWallets() {
        int userId = 1;
        String address = "address";
        User user = new User(userId, "user1");
        UserWallet userWallet = new UserWallet();
        userWallet.setId(1);
        userWallet.setAddress(address);
        userWallet.setWalletType(WalletTpyeEnum.ETH);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userWalletRepository.findByUserId(userId)).thenReturn(Collections.singletonList(userWallet));
        when(ethChain.getBalanceByAddress(address)).thenReturn(new BigInteger("1000000000000000000"));

        List<UserWalletResponse> responses = userWalletService.getAllUserWallets(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(address, responses.get(0).getAddress());
        assertEquals(new BigInteger("1000000000000000000"), responses.get(0).getBalanceWei());
        assertEquals(new BigDecimal("1"), responses.get(0).getBalanceEther());
    }

    @Test
    public void testGetUserBalance() {
        int userId = 1;
        String address = "address";
        BigInteger expectedBalance = new BigInteger("1000000000000000000");

        when(ethChain.getBalanceByAddress(address)).thenReturn(expectedBalance);

        BigInteger balance = userWalletService.getUserBalance(userId, address);

        assertEquals(expectedBalance, balance);
    }

    @Test
    public void testCreateWallet() {
        int userId = 1;
        User user = new User();
        user.setId(userId);
        Triple<String, String, String> triple = Triple.of("0x123", "privateKey", "publicKey");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ethChain.generateAddress()).thenReturn(triple);

        UserWalletResponse response = userWalletService.createWallet(userId);

        assertNotNull(response);
        assertEquals("0x123", response.getAddress());
        assertEquals("privateKey", response.getPrivateKey());
        assertEquals("publicKey", response.getPublicKey());
        assertEquals(new BigInteger("0"), response.getBalanceWei());
        assertEquals(new BigDecimal("0"), response.getBalanceEther());
        verify(userWalletRepository, times(1)).save(any(UserWallet.class));
    }

}

package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.dto.UserWalletResponse;
import com.pundix.wallet.task.enums.WalletStatusEnum;
import com.pundix.wallet.task.enums.WalletTpyeEnum;
import com.pundix.wallet.task.service.UserService;
import com.pundix.wallet.task.service.UserWalletService;
import com.pundix.wallet.task.utils.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserWalletControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private UserWalletController userWalletController;

    @Test
    public void testListUserWallets() {
        UserWalletResponse walletResponse1 = new UserWalletResponse(1, WalletTpyeEnum.ETH, "address1", "privateKey1", "publicKey1", WalletStatusEnum.NORMAL, "remark1", true, BigInteger.valueOf(1000), BigDecimal.valueOf(0.1));
        UserWalletResponse walletResponse2 = new UserWalletResponse(2, WalletTpyeEnum.ETH, "address2", "privateKey2", "publicKey2", WalletStatusEnum.NORMAL, "remark2", false, BigInteger.valueOf(2000), BigDecimal.valueOf(0.2));
        List<UserWalletResponse> walletResponses = Arrays.asList(walletResponse1, walletResponse2);

        when(userWalletService.getAllUserWallets(anyInt())).thenReturn(walletResponses);

        ApiResponse response = userWalletController.listUserWallets(1);

        assertEquals(200, response.getStatus());
        assertEquals(walletResponses, response.getData());
    }

    @Test
    public void testGetUserBalance() throws Exception {
        BigInteger balance = BigInteger.valueOf(1000);

        when(userWalletService.getUserBalance(anyInt(), anyString())).thenReturn(balance);

        ApiResponse response = userWalletController.getUserBalance(1, "testAddress");

        assertEquals(balance, response.getData());
    }

    @Test
    public void testCreateUserWallet() throws Exception {
        UserWalletResponse walletResponse = new UserWalletResponse(1, WalletTpyeEnum.ETH, "address1", "privateKey1", "publicKey1", WalletStatusEnum.NORMAL, "remark1", true, BigInteger.valueOf(1000), BigDecimal.valueOf(0.1));

        when(userWalletService.createWallet(anyInt())).thenReturn(walletResponse);

        ApiResponse response = userWalletController.createUserWallet(1);

        assertEquals(walletResponse, response.getData());
    }
}

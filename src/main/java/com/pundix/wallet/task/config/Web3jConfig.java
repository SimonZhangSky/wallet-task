package com.pundix.wallet.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3jConfig {

    private final String ETH_RPC_URL = "https://eth-mainnet.g.alchemy.com/v2/LANUTqbMxdeg_BuV3DoIpNPO3VuSmz8x";
    private final String ETH_WALLET_ADDRESS = "0x7A85147CB61E0C26F50F74DF664fb04F0824701c";
    private final String ETH_USDT_CONTRACT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7";

    @Bean
    public Web3j web3j() {
        // "https://mainnet.infura.io/v3/your_infura_project_id"
        return Web3j.build(new HttpService(ETH_RPC_URL));
    }

    @Bean
    public Admin admin() {
        // "https://mainnet.infura.io/v3/your_infura_project_id"
        return Admin.build(new HttpService(ETH_RPC_URL));
    }

}

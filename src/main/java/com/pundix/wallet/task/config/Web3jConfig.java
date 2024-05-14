package com.pundix.wallet.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3jConfig {

    private final String JSON_RPC_URL = "https://mainnet.infura.io/v3/d20d2f1b1f3b4eb7868f002272216072";

    @Bean
    public Web3j web3j() {
        // "https://mainnet.infura.io/v3/your_infura_project_id"
        return Web3j.build(new HttpService(JSON_RPC_URL));
    }

    @Bean
    public Admin admin() {
        // "https://mainnet.infura.io/v3/your_infura_project_id"
        return Admin.build(new HttpService(JSON_RPC_URL));
    }

}

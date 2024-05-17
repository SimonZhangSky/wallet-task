package com.pundix.wallet.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@MapperScan("com.pundix.wallet.task.dao")
@EnableAsync
@EnableScheduling
public class WalletTaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletTaskApplication.class, args);
    }
}

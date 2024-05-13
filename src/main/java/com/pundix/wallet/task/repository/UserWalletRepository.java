package com.pundix.wallet.task.repository;

import com.pundix.wallet.task.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {

    List<UserWallet> findByUserId(int userId);

}

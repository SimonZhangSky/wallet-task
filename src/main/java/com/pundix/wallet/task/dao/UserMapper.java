package com.pundix.wallet.task.dao;

import com.pundix.wallet.task.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMapper {

    List<User> findByName(String name);

}

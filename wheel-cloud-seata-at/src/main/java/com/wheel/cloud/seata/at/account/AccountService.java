package com.wheel.cloud.seata.at.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Transactional(transactionManager = "accountTxManager")
    public void deduct(Long userId, BigDecimal money) {
        int rows = accountMapper.deduct(userId, money);
        if (rows == 0) {
            throw new RuntimeException("余额不足: userId=" + userId + ", 需要=" + money);
        }
    }
}

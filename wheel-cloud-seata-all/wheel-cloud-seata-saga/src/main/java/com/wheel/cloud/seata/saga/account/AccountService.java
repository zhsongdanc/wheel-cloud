package com.wheel.cloud.seata.saga.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final JdbcTemplate accountJdbc;

    public AccountService(JdbcTemplate accountJdbc) {
        this.accountJdbc = accountJdbc;
    }

    /** 正向：扣余额 */
    @Transactional
    public boolean deduct(Long userId, BigDecimal money) {
        log.info("[Saga Account] deduct: userId={}, money={}", userId, money);
        int rows = accountJdbc.update(
            "UPDATE t_account SET used=used+?, residue=residue-? WHERE user_id=? AND residue>=?",
            money, money, userId, money
        );
        if (rows == 0) throw new RuntimeException("余额不足: userId=" + userId);
        return true;
    }

    /** 补偿：退还余额 */
    @Transactional
    public boolean compensateDeduct(Long userId, BigDecimal money) {
        log.info("[Saga Account] compensateDeduct: userId={}, money={}", userId, money);
        accountJdbc.update(
            "UPDATE t_account SET used=used-?, residue=residue+? WHERE user_id=? AND used>=?",
            money, money, userId, money
        );
        return true;
    }
}

package com.wheel.cloud.seata.tcc.account;

import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class AccountTccActionImpl implements AccountTccAction {

    private static final Logger log = LoggerFactory.getLogger(AccountTccActionImpl.class);

    private final JdbcTemplate accountJdbc;

    public AccountTccActionImpl(JdbcTemplate accountJdbc) {
        this.accountJdbc = accountJdbc;
    }

    @Override
    @Transactional
    public boolean tryDeduct(Long userId, BigDecimal money, BusinessActionContext context) {
        log.info("[Account TCC] Try: userId={}, money={}, xid={}", userId, money, context.getXid());
        int rows = accountJdbc.update(
            "UPDATE t_account SET residue = residue - ?, freeze = freeze + ? " +
            "WHERE user_id = ? AND residue >= ?",
            money, money, userId, money
        );
        if (rows == 0) {
            throw new RuntimeException("余额不足，TCC Try 失败: userId=" + userId);
        }
        log.info("[Account TCC] Try success");
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        Long userId = Long.valueOf(context.getActionContext("userId").toString());
        BigDecimal money = new BigDecimal(context.getActionContext("money").toString());
        log.info("[Account TCC] Confirm: userId={}, money={}, xid={}", userId, money, context.getXid());
        int rows = accountJdbc.update(
            "UPDATE t_account SET used = used + ?, freeze = freeze - ? WHERE user_id = ? AND freeze >= ?",
            money, money, userId, money
        );
        if (rows == 0) {
            log.warn("[Account TCC] Confirm: already confirmed, skip. userId={}", userId);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        Long userId = Long.valueOf(context.getActionContext("userId").toString());
        BigDecimal money = new BigDecimal(context.getActionContext("money").toString());
        log.info("[Account TCC] Cancel: userId={}, money={}, xid={}", userId, money, context.getXid());
        int rows = accountJdbc.update(
            "UPDATE t_account SET residue = residue + ?, freeze = freeze - ? WHERE user_id = ? AND freeze >= ?",
            money, money, userId, money
        );
        if (rows == 0) {
            log.warn("[Account TCC] Cancel: freeze=0, empty rollback. userId={}", userId);
        }
        return true;
    }
}

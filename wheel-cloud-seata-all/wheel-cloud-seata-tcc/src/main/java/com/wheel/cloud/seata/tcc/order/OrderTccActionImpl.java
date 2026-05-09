package com.wheel.cloud.seata.tcc.order;

import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Component
public class OrderTccActionImpl implements OrderTccAction {

    private static final Logger log = LoggerFactory.getLogger(OrderTccActionImpl.class);

    private final JdbcTemplate orderJdbc;

    public OrderTccActionImpl(JdbcTemplate orderJdbc) {
        this.orderJdbc = orderJdbc;
    }

    @Override
    @Transactional
    public boolean tryCreate(Long userId, Long productId, Integer count, BigDecimal money,
                             BusinessActionContext context) {
        log.info("[Order TCC] Try: userId={}, productId={}, xid={}", userId, productId, context.getXid());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        orderJdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO t_order (user_id, product_id, count, money, status) VALUES (?,?,?,?,0)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setInt(3, count);
            ps.setBigDecimal(4, money);
            return ps;
        }, keyHolder);
        long orderId = keyHolder.getKey().longValue();
        // 把 orderId 存到上下文，Confirm/Cancel 时用
        context.getActionContext().put("orderId", orderId);
        log.info("[Order TCC] Try success, orderId={}", orderId);
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        Long orderId = Long.valueOf(context.getActionContext("orderId").toString());
        log.info("[Order TCC] Confirm: orderId={}, xid={}", orderId, context.getXid());
        int rows = orderJdbc.update(
            "UPDATE t_order SET status = 1 WHERE id = ? AND status = 0",
            orderId
        );
        if (rows == 0) {
            log.warn("[Order TCC] Confirm: already confirmed. orderId={}", orderId);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        Object orderIdObj = context.getActionContext("orderId");
        if (orderIdObj == null) {
            // 空回滚：Try 阶段还没执行就 Cancel 了
            log.warn("[Order TCC] Cancel: empty rollback, no orderId in context");
            return true;
        }
        Long orderId = Long.valueOf(orderIdObj.toString());
        log.info("[Order TCC] Cancel: orderId={}, xid={}", orderId, context.getXid());
        orderJdbc.update("DELETE FROM t_order WHERE id = ? AND status = 0", orderId);
        return true;
    }
}

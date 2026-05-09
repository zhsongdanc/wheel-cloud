package com.wheel.cloud.seata.saga.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final JdbcTemplate orderJdbc;

    public OrderService(JdbcTemplate orderJdbc) {
        this.orderJdbc = orderJdbc;
    }

    /** 正向：创建订单 */
    @Transactional
    public Long createOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[Saga Order] createOrder: userId={}, productId={}", userId, productId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        orderJdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO t_order (user_id, product_id, count, money, status) VALUES (?,?,?,?,1)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId); ps.setLong(2, productId);
            ps.setInt(3, count); ps.setBigDecimal(4, money);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** 补偿：删除订单 */
    @Transactional
    public boolean compensateCreateOrder(Long orderId) {
        log.info("[Saga Order] compensateCreateOrder: orderId={}", orderId);
        orderJdbc.update("DELETE FROM t_order WHERE id=?", orderId);
        return true;
    }
}

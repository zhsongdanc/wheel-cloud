package com.wheel.cloud.seata.at.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 在 seata_order 库里创建订单（status=0），再更新为已完成（status=1）。
     * 用 @Transactional 保证本地事务；Seata 的 DataSourceProxy 会自动拦截并注册分支事务。
     */
    @Transactional(transactionManager = "orderTxManager")
    public Order createOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCount(count);
        order.setMoney(money);
        order.setStatus(0);
        orderMapper.insert(order);

        orderMapper.complete(order.getId());
        return order;
    }
}

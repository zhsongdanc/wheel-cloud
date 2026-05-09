package com.wheel.cloud.seata.xa.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    public OrderService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }

    @Transactional(transactionManager = "orderTxManager")
    public Order createOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        Order order = new Order();
        order.setUserId(userId); order.setProductId(productId);
        order.setCount(count); order.setMoney(money); order.setStatus(0);
        orderMapper.insert(order);
        orderMapper.complete(order.getId());
        return order;
    }
}

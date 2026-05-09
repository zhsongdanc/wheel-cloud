package com.wheel.cloud.seata.xa.common;

import com.wheel.cloud.seata.xa.account.AccountService;
import com.wheel.cloud.seata.xa.order.Order;
import com.wheel.cloud.seata.xa.order.OrderService;
import com.wheel.cloud.seata.xa.stock.StockService;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * XA 模式下单主流程
 *
 * 代码结构和 AT 完全一样，差异只在数据源层（DataSourceProxyXA）。
 * XA vs AT 的本质区别：
 *   AT — Seata 解析 SQL 生成 undo_log，一阶段直接提交本地事务（不持有全局锁），性能好
 *   XA — 数据库原生两阶段协议，一阶段 prepare 持有数据库锁直到全局提交/回滚，强一致但性能差
 *
 * 观察点：XA 模式下没有 undo_log 表写入，回滚由数据库引擎自己完成（XA Rollback）
 */
@Service
public class PlaceOrderXaService {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderXaService.class);

    private final StockService stockService;
    private final AccountService accountService;
    private final OrderService orderService;

    public PlaceOrderXaService(StockService stockService, AccountService accountService, OrderService orderService) {
        this.stockService = stockService;
        this.accountService = accountService;
        this.orderService = orderService;
    }

    @GlobalTransactional(name = "xa-place-order-commit", rollbackFor = Exception.class)
    public Order placeOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[XA] placeOrder start");
        stockService.deduct(productId, count);
        accountService.deduct(userId, money);
        Order order = orderService.createOrder(userId, productId, count, money);
        log.info("[XA] placeOrder done, orderId={}", order.getId());
        return order;
    }

    @GlobalTransactional(name = "xa-place-order-rollback", rollbackFor = Exception.class)
    public void placeOrderWithRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        stockService.deduct(productId, count);
        accountService.deduct(userId, money.add(new BigDecimal("999999")));
        orderService.createOrder(userId, productId, count, money);
    }

    @GlobalTransactional(name = "xa-place-order-manual-rollback", rollbackFor = Exception.class)
    public void placeOrderManualRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        stockService.deduct(productId, count);
        accountService.deduct(userId, money);
        orderService.createOrder(userId, productId, count, money);
        throw new RuntimeException("手动触发 XA 全局回滚，观察数据库 XA Rollback（无 undo_log）");
    }
}

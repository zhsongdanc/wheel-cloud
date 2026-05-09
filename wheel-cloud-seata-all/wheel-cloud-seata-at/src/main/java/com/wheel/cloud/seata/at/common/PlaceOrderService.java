package com.wheel.cloud.seata.at.common;

import com.wheel.cloud.seata.at.account.AccountService;
import com.wheel.cloud.seata.at.order.Order;
import com.wheel.cloud.seata.at.order.OrderService;
import com.wheel.cloud.seata.at.stock.StockService;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 跨库"下单"主流程：seata_order + seata_stock + seata_account 三个库。
 *
 * @GlobalTransactional 由 Seata TM 开启全局事务，xid 通过 RootContext 传播到每个
 * 本地 @Transactional，各库的 DataSourceProxy 自动向 TC 注册分支事务并写 undo_log。
 * 全局提交时三个库同时提交；全局回滚时 TC 驱动各 RM 用 undo_log 反向补偿。
 */
@Service
public class PlaceOrderService {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderService.class);

    private final OrderService orderService;
    private final StockService stockService;
    private final AccountService accountService;

    public PlaceOrderService(OrderService orderService,
                             StockService stockService,
                             AccountService accountService) {
        this.orderService = orderService;
        this.stockService = stockService;
        this.accountService = accountService;
    }

    /**
     * 正常下单：三步全部成功，全局提交。
     */
    @GlobalTransactional(name = "place-order-commit", rollbackFor = Exception.class)
    public Order placeOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[PlaceOrder] start: userId={}, productId={}, count={}, money={}", userId, productId, count, money);

        // 1. 扣库存（seata_stock）
        stockService.deduct(productId, count);
        log.info("[PlaceOrder] stock deducted");

        // 2. 扣余额（seata_account）
        accountService.deduct(userId, money);
        log.info("[PlaceOrder] account deducted");

        // 3. 创建订单（seata_order）
        Order order = orderService.createOrder(userId, productId, count, money);
        log.info("[PlaceOrder] order created: id={}", order.getId());

        return order;
    }

    /**
     * 模拟回滚场景：库存扣成功，余额不足时抛异常。
     * Seata 会驱动 TC 回滚已提交的库存分支（用 undo_log 反向 SQL）。
     */
    @GlobalTransactional(name = "place-order-rollback", rollbackFor = Exception.class)
    public void placeOrderWithRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[PlaceOrderRollback] start: userId={}, productId={}, count={}, money={}", userId, productId, count, money);

        // 1. 扣库存（会成功）
        stockService.deduct(productId, count);
        log.info("[PlaceOrderRollback] stock deducted — about to fail on account");

        // 2. 故意传一个巨额金额触发余额不足
        accountService.deduct(userId, money.add(new BigDecimal("999999")));

        // 走不到这里，TC 会回滚步骤1的库存
        orderService.createOrder(userId, productId, count, money);
    }

    /**
     * 模拟手动抛异常回滚：三步业务都成功了，但最后手动抛出异常。
     * 观察三个库的数据是否全部回滚，undo_log 是否被清空。
     */
    @GlobalTransactional(name = "place-order-manual-rollback", rollbackFor = Exception.class)
    public void placeOrderManualRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[PlaceOrderManualRollback] start");

        stockService.deduct(productId, count);
        accountService.deduct(userId, money);
        orderService.createOrder(userId, productId, count, money);

        log.info("[PlaceOrderManualRollback] all local ops done — throwing to trigger global rollback");
        throw new RuntimeException("手动触发全局回滚，验证三个库数据是否恢复");
    }
}

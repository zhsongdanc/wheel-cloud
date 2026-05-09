package com.wheel.cloud.seata.tcc.common;

import com.wheel.cloud.seata.tcc.account.AccountTccAction;
import com.wheel.cloud.seata.tcc.order.OrderTccAction;
import com.wheel.cloud.seata.tcc.stock.StockTccAction;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * TCC 模式下单主流程
 *
 * 和 AT 模式最大的区别：
 *   AT — 只需调业务方法，Seata 自动生成 undo_log，全局回滚时自动反向补偿
 *   TCC — 每个参与者都要手写 Try/Confirm/Cancel，由开发者保证幂等和空回滚
 *
 * TCC 性能更好（不需要加全局行锁），但开发成本高。
 */
@Service
public class PlaceOrderTccService {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderTccService.class);

    private final StockTccAction stockTccAction;
    private final AccountTccAction accountTccAction;
    private final OrderTccAction orderTccAction;

    public PlaceOrderTccService(StockTccAction stockTccAction,
                                AccountTccAction accountTccAction,
                                OrderTccAction orderTccAction) {
        this.stockTccAction = stockTccAction;
        this.accountTccAction = accountTccAction;
        this.orderTccAction = orderTccAction;
    }

    /** 正常下单：三个 Try 全成功 → TC 驱动三个 Confirm */
    @GlobalTransactional(name = "tcc-place-order-commit", rollbackFor = Exception.class)
    public void placeOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[TCC] start placeOrder: userId={}, productId={}", userId, productId);
        stockTccAction.tryDeduct(productId, count, null);
        accountTccAction.tryDeduct(userId, money, null);
        orderTccAction.tryCreate(userId, productId, count, money, null);
        log.info("[TCC] all Try done, TC will drive Confirm");
    }

    /** 余额不足触发回滚：库存 Try 成功后账户 Try 失败 → TC 驱动 stockTccAction.cancel */
    @GlobalTransactional(name = "tcc-place-order-rollback", rollbackFor = Exception.class)
    public void placeOrderWithRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[TCC] start placeOrderWithRollback");
        stockTccAction.tryDeduct(productId, count, null);
        // 传入巨额触发余额不足
        accountTccAction.tryDeduct(userId, money.add(new BigDecimal("999999")), null);
        orderTccAction.tryCreate(userId, productId, count, money, null);
    }

    /** 三步 Try 都成功，最后手动抛异常 → 三个 Cancel 全被调用 */
    @GlobalTransactional(name = "tcc-place-order-manual-rollback", rollbackFor = Exception.class)
    public void placeOrderManualRollback(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[TCC] start placeOrderManualRollback");
        stockTccAction.tryDeduct(productId, count, null);
        accountTccAction.tryDeduct(userId, money, null);
        orderTccAction.tryCreate(userId, productId, count, money, null);
        throw new RuntimeException("手动触发 TCC 全局回滚，观察三个 Cancel 是否都被调用");
    }
}

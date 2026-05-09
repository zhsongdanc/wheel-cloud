package com.wheel.cloud.seata.saga.common;

import com.wheel.cloud.seata.saga.account.AccountService;
import com.wheel.cloud.seata.saga.order.OrderService;
import com.wheel.cloud.seata.saga.stock.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saga 模式下单主流程（编排式，不依赖 Seata 状态机引擎）
 *
 * Saga 有两种实现方式：
 *   1. 编排式（Choreography）— 各服务自己发事件，互相触发，无中心调度
 *   2. 协调式（Orchestration）— 有一个 Saga 协调者（这里用 Seata StateMachine）负责调度
 *
 * 这里用最直白的编程式 Saga 让你看清楚逻辑：
 *   正向步骤顺序执行，任意一步失败则倒序执行已完成步骤的补偿方法。
 *
 * 和 AT/TCC/XA 的本质区别：
 *   Saga 不保证隔离性（其他事务能看到中间状态），适合长事务、跨服务、允许最终一致的场景。
 */
@Service
public class PlaceOrderSagaService {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderSagaService.class);

    private final StockService stockService;
    private final AccountService accountService;
    private final OrderService orderService;

    public PlaceOrderSagaService(StockService stockService, AccountService accountService, OrderService orderService) {
        this.stockService = stockService;
        this.accountService = accountService;
        this.orderService = orderService;
    }

    /**
     * 正常下单：三步全成功
     */
    public Map<String, Object> placeOrder(Long userId, Long productId, Integer count, BigDecimal money) {
        log.info("[Saga] placeOrder start");
        boolean stockDone = false;
        boolean accountDone = false;
        Long orderId = null;
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Step 1: 扣库存
            stockService.deduct(productId, count);
            stockDone = true;
            log.info("[Saga] Step1 stock deducted");

            // Step 2: 扣余额
            accountService.deduct(userId, money);
            accountDone = true;
            log.info("[Saga] Step2 account deducted");

            // Step 3: 创建订单
            orderId = orderService.createOrder(userId, productId, count, money);
            log.info("[Saga] Step3 order created, orderId={}", orderId);

            result.put("result", "SUCCESS");
            result.put("orderId", orderId);

        } catch (Exception e) {
            log.error("[Saga] failed: {}, starting compensation", e.getMessage());
            // 倒序补偿已完成的步骤
            if (accountDone) {
                accountService.compensateDeduct(userId, money);
                log.info("[Saga] compensated account");
            }
            if (stockDone) {
                stockService.compensateDeduct(productId, count);
                log.info("[Saga] compensated stock");
            }
            result.put("result", "SAGA_COMPENSATED");
            result.put("reason", e.getMessage());
        }
        return result;
    }

    /**
     * 触发补偿：余额不足，已扣的库存被补偿回来
     */
    public Map<String, Object> placeOrderWithCompensation(Long userId, Long productId, Integer count, BigDecimal money) {
        return placeOrder(userId, productId, count, money.add(new BigDecimal("999999")));
    }
}

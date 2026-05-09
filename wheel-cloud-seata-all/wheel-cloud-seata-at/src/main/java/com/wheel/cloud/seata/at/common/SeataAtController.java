package com.wheel.cloud.seata.at.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wheel.cloud.seata.at.account.Account;
import com.wheel.cloud.seata.at.account.AccountMapper;
import com.wheel.cloud.seata.at.order.Order;
import com.wheel.cloud.seata.at.order.OrderMapper;
import com.wheel.cloud.seata.at.stock.Stock;
import com.wheel.cloud.seata.at.stock.StockMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试入口
 *
 * GET /seata/at/commit          — 正常下单，观察三库数据变化
 * GET /seata/at/rollback        — 余额不足触发全局回滚，观察库存是否恢复
 * GET /seata/at/manual-rollback — 三步都成功后手动抛异常，观察全部回滚
 * GET /seata/at/state           — 查看三库当前数据快照
 * POST /seata/at/reset          — 重置数据（方便反复测试）
 */
@RestController
@RequestMapping("/seata/at")
public class SeataAtController {

    private final PlaceOrderService placeOrderService;
    private final OrderMapper orderMapper;
    private final StockMapper stockMapper;
    private final AccountMapper accountMapper;

    public SeataAtController(PlaceOrderService placeOrderService,
                             OrderMapper orderMapper,
                             StockMapper stockMapper,
                             AccountMapper accountMapper) {
        this.placeOrderService = placeOrderService;
        this.orderMapper = orderMapper;
        this.stockMapper = stockMapper;
        this.accountMapper = accountMapper;
    }

    /** 正常下单——三库全提交 */
    @GetMapping("/commit")
    public Map<String, Object> commit(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Order order = placeOrderService.placeOrder(userId, productId, count, money);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", "SUCCESS");
        result.put("orderId", order.getId());
        result.put("tip", "查看 /seata/at/state 确认三库数据均已变更");
        return result;
    }

    /** 余额不足触发全局回滚——库存分支应被 undo_log 反向补偿 */
    @GetMapping("/rollback")
    public Map<String, Object> rollback(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            placeOrderService.placeOrderWithRollback(userId, productId, count, money);
            result.put("result", "SUCCESS（不应到达这里）");
        } catch (Exception e) {
            result.put("result", "ROLLBACK");
            result.put("reason", e.getMessage());
            result.put("tip", "查看 /seata/at/state 确认库存已恢复，订单未创建");
        }
        return result;
    }

    /** 手动抛异常——三步全部成功但最终全局回滚 */
    @GetMapping("/manual-rollback")
    public Map<String, Object> manualRollback(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "50") BigDecimal money) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            placeOrderService.placeOrderManualRollback(userId, productId, count, money);
        } catch (Exception e) {
            result.put("result", "ROLLBACK");
            result.put("reason", e.getMessage());
            result.put("tip", "三个库的数据应全部回滚，undo_log 被清空");
        }
        return result;
    }

    /** 当前数据快照 */
    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Stock> stocks = stockMapper.selectList(null);
        result.put("stocks", stocks);

        List<Account> accounts = accountMapper.selectList(null);
        result.put("accounts", accounts);

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getId).last("LIMIT 10"));
        result.put("latestOrders", orders);

        return result;
    }

    /** 重置测试数据 */
    @GetMapping("/reset")
    public Map<String, Object> reset() {
        stockMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Stock>()
                        .set(Stock::getUsed, 0)
                        .set(Stock::getResidue, 100)
                        .eq(Stock::getProductId, 1));
        stockMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Stock>()
                        .set(Stock::getUsed, 0)
                        .set(Stock::getResidue, 50)
                        .eq(Stock::getProductId, 2));
        accountMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Account>()
                        .set(Account::getUsed, new BigDecimal("0"))
                        .set(Account::getResidue, new BigDecimal("1000"))
                        .eq(Account::getUserId, 1));
        accountMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Account>()
                        .set(Account::getUsed, new BigDecimal("0"))
                        .set(Account::getResidue, new BigDecimal("500"))
                        .eq(Account::getUserId, 2));
        orderMapper.delete(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", "RESET OK");
        result.put("tip", "库存、账户已恢复初始值，订单表已清空");
        return result;
    }
}

package com.wheel.cloud.seata.xa.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wheel.cloud.seata.xa.account.Account;
import com.wheel.cloud.seata.xa.account.AccountMapper;
import com.wheel.cloud.seata.xa.order.Order;
import com.wheel.cloud.seata.xa.order.OrderMapper;
import com.wheel.cloud.seata.xa.stock.Stock;
import com.wheel.cloud.seata.xa.stock.StockMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /seata/xa/commit          — 正常下单
 * GET /seata/xa/rollback        — 余额不足回滚（无 undo_log，由数据库 XA Rollback 完成）
 * GET /seata/xa/manual-rollback — 手动抛异常全局回滚
 * GET /seata/xa/state           — 查看数据
 * POST /seata/xa/reset          — 重置
 */
@RestController
@RequestMapping("/seata/xa")
public class SeataXaController {

    private final PlaceOrderXaService placeOrderXaService;
    private final OrderMapper orderMapper;
    private final StockMapper stockMapper;
    private final AccountMapper accountMapper;

    public SeataXaController(PlaceOrderXaService placeOrderXaService,
                              OrderMapper orderMapper, StockMapper stockMapper, AccountMapper accountMapper) {
        this.placeOrderXaService = placeOrderXaService;
        this.orderMapper = orderMapper;
        this.stockMapper = stockMapper;
        this.accountMapper = accountMapper;
    }

    @GetMapping("/commit")
    public Map<String, Object> commit(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Order order = placeOrderXaService.placeOrder(userId, productId, count, money);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("result", "SUCCESS"); r.put("orderId", order.getId());
        r.put("tip", "XA 模式无 undo_log，查看 seata_order.undo_log 应为空");
        return r;
    }

    @GetMapping("/rollback")
    public Map<String, Object> rollback(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Map<String, Object> r = new LinkedHashMap<>();
        try { placeOrderXaService.placeOrderWithRollback(userId, productId, count, money); }
        catch (Exception e) {
            r.put("result", "ROLLBACK"); r.put("reason", e.getMessage());
            r.put("tip", "数据库通过 XA Rollback 恢复，不依赖 undo_log");
        }
        return r;
    }

    @GetMapping("/manual-rollback")
    public Map<String, Object> manualRollback(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "50") BigDecimal money) {
        Map<String, Object> r = new LinkedHashMap<>();
        try { placeOrderXaService.placeOrderManualRollback(userId, productId, count, money); }
        catch (Exception e) { r.put("result", "ROLLBACK"); r.put("reason", e.getMessage()); }
        return r;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("stocks", stockMapper.selectList(null));
        r.put("accounts", accountMapper.selectList(null));
        r.put("latestOrders", orderMapper.selectList(
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getId).last("LIMIT 10")));
        return r;
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        stockMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Stock>()
                .set(Stock::getUsed, 0).set(Stock::getResidue, 100).eq(Stock::getProductId, 1));
        stockMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Stock>()
                .set(Stock::getUsed, 0).set(Stock::getResidue, 50).eq(Stock::getProductId, 2));
        accountMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Account>()
                .set(Account::getUsed, new BigDecimal("0")).set(Account::getResidue, new BigDecimal("1000")).eq(Account::getUserId, 1));
        accountMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Account>()
                .set(Account::getUsed, new BigDecimal("0")).set(Account::getResidue, new BigDecimal("500")).eq(Account::getUserId, 2));
        orderMapper.delete(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("result", "RESET OK");
        return r;
    }
}

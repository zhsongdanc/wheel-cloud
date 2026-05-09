package com.wheel.cloud.seata.tcc.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /seata/tcc/commit          — 正常下单，三个 Confirm 被调用
 * GET /seata/tcc/rollback        — 余额不足，库存 Cancel 被调用
 * GET /seata/tcc/manual-rollback — 手动抛异常，三个 Cancel 全被调用
 * GET /seata/tcc/state           — 查看数据（重点看 freeze 字段是否归零）
 * POST /seata/tcc/reset          — 重置
 */
@RestController
@RequestMapping("/seata/tcc")
public class SeataTccController {

    private final PlaceOrderTccService placeOrderTccService;
    private final JdbcTemplate orderJdbc;
    private final JdbcTemplate stockJdbc;
    private final JdbcTemplate accountJdbc;

    public SeataTccController(PlaceOrderTccService placeOrderTccService,
                               JdbcTemplate orderJdbc,
                               JdbcTemplate stockJdbc,
                               JdbcTemplate accountJdbc) {
        this.placeOrderTccService = placeOrderTccService;
        this.orderJdbc = orderJdbc;
        this.stockJdbc = stockJdbc;
        this.accountJdbc = accountJdbc;
    }

    @GetMapping("/commit")
    public Map<String, Object> commit(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        placeOrderTccService.placeOrder(userId, productId, count, money);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("result", "SUCCESS");
        r.put("tip", "查看 /seata/tcc/state，freeze 字段应为 0（Confirm 已清空冻结）");
        return r;
    }

    @GetMapping("/rollback")
    public Map<String, Object> rollback(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            placeOrderTccService.placeOrderWithRollback(userId, productId, count, money);
        } catch (Exception e) {
            r.put("result", "ROLLBACK");
            r.put("reason", e.getMessage());
            r.put("tip", "库存 freeze 应归零，residue 已恢复；订单未创建");
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
        try {
            placeOrderTccService.placeOrderManualRollback(userId, productId, count, money);
        } catch (Exception e) {
            r.put("result", "ROLLBACK");
            r.put("reason", e.getMessage());
            r.put("tip", "三个 Cancel 全被调用，freeze 全部归零");
        }
        return r;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("stocks", stockJdbc.queryForList("SELECT * FROM t_stock"));
        r.put("accounts", accountJdbc.queryForList("SELECT * FROM t_account"));
        r.put("latestOrders", orderJdbc.queryForList("SELECT * FROM t_order ORDER BY id DESC LIMIT 10"));
        return r;
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        stockJdbc.update("UPDATE t_stock SET used=0, residue=100, freeze=0 WHERE product_id=1");
        stockJdbc.update("UPDATE t_stock SET used=0, residue=50, freeze=0 WHERE product_id=2");
        accountJdbc.update("UPDATE t_account SET used=0, residue=1000, freeze=0 WHERE user_id=1");
        accountJdbc.update("UPDATE t_account SET used=0, residue=500, freeze=0 WHERE user_id=2");
        orderJdbc.update("DELETE FROM t_order");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("result", "RESET OK");
        return r;
    }
}

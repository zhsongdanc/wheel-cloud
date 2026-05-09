package com.wheel.cloud.seata.saga.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /seata/saga/commit       — 正常下单，三步全成功
 * GET /seata/saga/compensate   — 余额不足，触发 Saga 补偿（倒序回滚已完成步骤）
 * GET /seata/saga/state        — 查看数据
 * POST /seata/saga/reset       — 重置
 */
@RestController
@RequestMapping("/seata/saga")
public class SeataSagaController {

    private final PlaceOrderSagaService placeOrderSagaService;
    private final JdbcTemplate orderJdbc;
    private final JdbcTemplate stockJdbc;
    private final JdbcTemplate accountJdbc;

    public SeataSagaController(PlaceOrderSagaService placeOrderSagaService,
                                JdbcTemplate orderJdbc, JdbcTemplate stockJdbc, JdbcTemplate accountJdbc) {
        this.placeOrderSagaService = placeOrderSagaService;
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
        return placeOrderSagaService.placeOrder(userId, productId, count, money);
    }

    @GetMapping("/compensate")
    public Map<String, Object> compensate(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "2") Integer count,
            @RequestParam(defaultValue = "100") BigDecimal money) {
        Map<String, Object> r = placeOrderSagaService.placeOrderWithCompensation(userId, productId, count, money);
        r.put("tip", "Saga 补偿：库存已倒序归还，账户未扣（Step2失败，Step1已补偿）");
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

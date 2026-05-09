package com.wheel.cloud.seata.saga.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga 库存服务
 *
 * Saga 模式要求每个正向操作都有对应的补偿操作：
 *   deduct（正向）     ↔  compensateDeduct（补偿）
 *
 * 补偿操作必须幂等，因为 TC 在网络异常时可能重试。
 * Saga 不锁资源，性能最好，但隔离性最弱（其他事务可以看到中间状态）。
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private final JdbcTemplate stockJdbc;

    public StockService(JdbcTemplate stockJdbc) {
        this.stockJdbc = stockJdbc;
    }

    /** 正向：扣库存 */
    @Transactional
    public boolean deduct(Long productId, Integer count) {
        log.info("[Saga Stock] deduct: productId={}, count={}", productId, count);
        int rows = stockJdbc.update(
            "UPDATE t_stock SET used=used+?, residue=residue-? WHERE product_id=? AND residue>=?",
            count, count, productId, count
        );
        if (rows == 0) throw new RuntimeException("库存不足: productId=" + productId);
        return true;
    }

    /** 补偿：归还库存（幂等：多次执行结果一致，通过检查 used >= count 防止负数） */
    @Transactional
    public boolean compensateDeduct(Long productId, Integer count) {
        log.info("[Saga Stock] compensateDeduct: productId={}, count={}", productId, count);
        stockJdbc.update(
            "UPDATE t_stock SET used=used-?, residue=residue+? WHERE product_id=? AND used>=?",
            count, count, productId, count
        );
        return true;
    }
}

package com.wheel.cloud.seata.tcc.stock;

import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * TCC 库存实现
 *
 * 用 JdbcTemplate 直接操作，不走 DataSourceProxy，因为 TCC 模式由业务代码手动管理补偿逻辑，
 * 不需要 Seata 自动生成 undo_log。
 */
@Component
public class StockTccActionImpl implements StockTccAction {

    private static final Logger log = LoggerFactory.getLogger(StockTccActionImpl.class);

    private final JdbcTemplate stockJdbc;

    public StockTccActionImpl(JdbcTemplate stockJdbc) {
        this.stockJdbc = stockJdbc;
    }

    @Override
    @Transactional
    public boolean tryDeduct(Long productId, Integer count, BusinessActionContext context) {
        log.info("[Stock TCC] Try: productId={}, count={}, xid={}", productId, count, context.getXid());
        // 检查库存并冻结：residue -= count, freeze += count
        int rows = stockJdbc.update(
            "UPDATE t_stock SET residue = residue - ?, freeze = freeze + ? " +
            "WHERE product_id = ? AND residue >= ?",
            count, count, productId, count
        );
        if (rows == 0) {
            throw new RuntimeException("库存不足，TCC Try 失败: productId=" + productId);
        }
        log.info("[Stock TCC] Try success");
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        Long productId = Long.valueOf(context.getActionContext("productId").toString());
        Integer count = Integer.valueOf(context.getActionContext("count").toString());
        log.info("[Stock TCC] Confirm: productId={}, count={}, xid={}", productId, count, context.getXid());
        // 提交：清掉冻结，used += count
        int rows = stockJdbc.update(
            "UPDATE t_stock SET used = used + ?, freeze = freeze - ? WHERE product_id = ? AND freeze >= ?",
            count, count, productId, count
        );
        if (rows == 0) {
            // 幂等：freeze 已经被清过了，说明已 confirm，直接返回 true
            log.warn("[Stock TCC] Confirm: already confirmed or freeze=0, skip. productId={}", productId);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        Long productId = Long.valueOf(context.getActionContext("productId").toString());
        Integer count = Integer.valueOf(context.getActionContext("count").toString());
        log.info("[Stock TCC] Cancel: productId={}, count={}, xid={}", productId, count, context.getXid());
        // 回滚：将冻结归还到 residue
        int rows = stockJdbc.update(
            "UPDATE t_stock SET residue = residue + ?, freeze = freeze - ? WHERE product_id = ? AND freeze >= ?",
            count, count, productId, count
        );
        if (rows == 0) {
            // 幂等：freeze 已经是 0，可能是空回滚（Try 没执行就 Cancel）
            log.warn("[Stock TCC] Cancel: freeze=0, maybe empty rollback. productId={}", productId);
        }
        return true;
    }
}

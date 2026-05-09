package com.wheel.cloud.seata.tcc.stock;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * TCC 库存服务接口
 *
 * TCC 三阶段语义：
 *   Try     — 检查库存是否足够，将 count 从 residue 移入 freeze（冻结），不真正扣减
 *   Confirm — 全局提交时调用，将 freeze 清零（已在 Try 里扣了 residue，这里只清 freeze）
 *   Cancel  — 全局回滚时调用，将 freeze 归还到 residue
 *
 * 这样设计的好处：Try 阶段资源已被锁定，其他事务无法超卖；
 * 即使 Confirm/Cancel 网络抖动需要重试，因为操作的是 freeze 字段，天然幂等。
 */
@LocalTCC
public interface StockTccAction {

    @TwoPhaseBusinessAction(name = "stockTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(@BusinessActionContextParameter(paramName = "productId") Long productId,
                      @BusinessActionContextParameter(paramName = "count") Integer count,
                      BusinessActionContext context);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

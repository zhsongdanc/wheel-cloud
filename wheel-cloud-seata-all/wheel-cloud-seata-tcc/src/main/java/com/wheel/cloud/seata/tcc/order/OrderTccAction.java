package com.wheel.cloud.seata.tcc.order;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * TCC 订单服务接口
 *
 * Try     — 插入 status=0（创建中）的订单
 * Confirm — 将订单 status 更新为 1（已完成）
 * Cancel  — 删除 Try 阶段插入的订单
 */
@LocalTCC
public interface OrderTccAction {

    @TwoPhaseBusinessAction(name = "orderTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryCreate(@BusinessActionContextParameter(paramName = "userId") Long userId,
                      @BusinessActionContextParameter(paramName = "productId") Long productId,
                      @BusinessActionContextParameter(paramName = "count") Integer count,
                      @BusinessActionContextParameter(paramName = "money") BigDecimal money,
                      BusinessActionContext context);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

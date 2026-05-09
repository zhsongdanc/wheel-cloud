package com.wheel.cloud.seata.tcc.account;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * TCC 账户服务接口
 *
 * Try     — 检查余额，将 money 从 residue 移入 freeze
 * Confirm — 清零 freeze（扣款已在 Try 里完成）
 * Cancel  — 将 freeze 归还到 residue
 */
@LocalTCC
public interface AccountTccAction {

    @TwoPhaseBusinessAction(name = "accountTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(@BusinessActionContextParameter(paramName = "userId") Long userId,
                      @BusinessActionContextParameter(paramName = "money") BigDecimal money,
                      BusinessActionContext context);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

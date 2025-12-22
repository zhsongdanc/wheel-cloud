package com.wheel.cloud.hystrix.invoke;

import com.wheel.cloud.hystrix.config.CircuitBreakerManager;
import com.wheel.cloud.hystrix.enums.IsolationTypeEnum;

public class InvokeStrategyFactory {

    public static InvokeStrategy getInvokeStrategy(IsolationTypeEnum invokeStrategyEnum, CircuitBreakerManager circuitBreakerManager) {
        if (invokeStrategyEnum == IsolationTypeEnum.SEMAPHORE) {
            return new SemaphoreInvokeStrategy(circuitBreakerManager);
        }
        return new ThreadPoolInvokeStrategy(circuitBreakerManager);
    }
}

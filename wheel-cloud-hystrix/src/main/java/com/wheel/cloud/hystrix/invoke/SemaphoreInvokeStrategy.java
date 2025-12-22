package com.wheel.cloud.hystrix.invoke;

import com.wheel.cloud.hystrix.anno.CommandParser;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.anno.HystrixProperty;
import com.wheel.cloud.hystrix.config.CircuitBreakerManager;
import com.wheel.cloud.hystrix.enums.IsolationTypeEnum;
import com.wheel.cloud.hystrix.exception.ExecuteTaskException;
import com.wheel.cloud.hystrix.exception.SemaphoreExceedLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodProxy;

import java.util.concurrent.Semaphore;

@Slf4j
public class SemaphoreInvokeStrategy implements InvokeStrategy{

    private CircuitBreakerManager circuitBreakerManager;

    public SemaphoreInvokeStrategy(CircuitBreakerManager circuitBreakerManager){
        this.circuitBreakerManager = circuitBreakerManager;
    }

    @Override
    public Object invoke(String groupKey, Object o, MethodProxy methodProxy, Object[] args, HystrixCommand annotation) throws Throwable {
        HystrixProperty[] hystrixProperties = annotation.commandProperties();
        int maxSemaphore = CommandParser.parseMaxSemaphore(hystrixProperties);
        Semaphore semaphore = new Semaphore(maxSemaphore);
        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            try {
                return methodProxy.invoke(o, args);
            } catch (Throwable e) {
                log.error("semaphore invoke error", e);
                throw new ExecuteTaskException(e);
            } finally {
                semaphore.release();
            }
        }
        throw new SemaphoreExceedLimitException("semaphore exceed limit");
    }

    @Override
    public IsolationTypeEnum getIsolation() {
        return IsolationTypeEnum.SEMAPHORE;
    }
}

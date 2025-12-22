package com.wheel.cloud.hystrix.invoke;

import com.wheel.cloud.hystrix.anno.CommandParser;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.config.CircuitBreakerManager;
import com.wheel.cloud.hystrix.enums.IsolationTypeEnum;
import com.wheel.cloud.hystrix.exception.ExecuteTaskException;
import com.wheel.cloud.hystrix.property.ThreadPoolProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodProxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ThreadPoolInvokeStrategy implements InvokeStrategy {

    private CircuitBreakerManager circuitBreakerManager;


    public ThreadPoolInvokeStrategy(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
    }


    @Override
    public Object invoke(String groupKey, Object o, MethodProxy methodProxy, Object[] args, HystrixCommand annotation) throws Throwable {

        ThreadPoolProperty threadPoolProperty = CommandParser.parseHystrixProperties(annotation.threadPoolProperties());

        return invokeTargetMethodByThreadPool(groupKey, o, methodProxy, args, threadPoolProperty);

    }

    @Override
    public IsolationTypeEnum getIsolation() {
        return IsolationTypeEnum.THREAD;
    }

    private Object invokeTargetMethodByThreadPool(String groupKey, Object o, MethodProxy methodProxy, Object[] args,
                                                  ThreadPoolProperty threadPoolProperty) throws Throwable {
        ExecutorService executor = circuitBreakerManager.getExecutor(groupKey, threadPoolProperty);
        Future<Object> invokeFuture = null;
        try {
            invokeFuture = executor.submit(() -> {
                try {
                    return methodProxy.invoke(o, args);
                } catch (Throwable e) {
                    throw new ExecuteTaskException(e);
                }
            });
            return invokeFuture.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            invokeFuture.cancel(true);
            log.error("hystrix proxy invokeTargetMethodByThreadPool error", e);
            throw e;
        } catch (InterruptedException e) {
            invokeFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecuteTaskException e) {
            throw e.getCause();
        }

    }
}

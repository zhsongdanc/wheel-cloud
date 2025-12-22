package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.exception.ExecuteTaskException;
import com.wheel.cloud.hystrix.exception.ForbiddenRequestException;
import com.wheel.cloud.hystrix.spring.HystrixProperties;
import com.wheel.cloud.hystrix.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HystrixMethodInterceptor implements MethodInterceptor {

    private CircuitBreakerManager circuitBreakerManager;

    public HystrixMethodInterceptor(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (method.isAnnotationPresent(HystrixCommand.class)) {
            log.info("hystrix proxy intercept,className:{},methodName:{}", o.getClass().getName(), method.getName());
            HystrixCommand annotation = method.getAnnotation(HystrixCommand.class);
            String fallbackMethodName = annotation.fallbackMethod();

            String methodKey = ClassUtil.generateMethodKey(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes());

            Object res = null;
            long startInvokeTime = System.currentTimeMillis();
            try {
                if (!allowRequest(methodKey)) {
                    throw new ForbiddenRequestException("circuit breaker is open");
                }
                res = invokeTargetMethodByThreadPool(methodKey, o, methodProxy, args);
                recordSuccess(methodKey, startInvokeTime, System.currentTimeMillis());
            } catch (Throwable throwable) {
                log.error("hystrix proxy fallbackMethod invoke error, but use defalut method");
                if (StringUtils.isNotBlank(fallbackMethodName)) {
                    Method fallbackMethod = findFallbackMethod(method.getDeclaringClass(), fallbackMethodName, method.getParameterTypes());
                    fallbackMethod.setAccessible(true);
                    try {
                        res = fallbackMethod.invoke(o, args);
                    } catch (Exception fallbackException) {
                        throw new ForbiddenRequestException("fallback method invoke error", fallbackException);
                    }
                }
                if (throwable instanceof ForbiddenRequestException) {
                    // nothing to do
                } else {
                    recordFailed(methodKey, startInvokeTime, throwable.getClass().getName(), throwable.getMessage());
                }
            }

            log.info("hystrix proxy intercept end,className:{},methodName:{}", o.getClass().getName(), method.getName());
            return res;
        }

        return methodProxy.invokeSuper(o, args);
    }

    private Object invokeTargetMethodByThreadPool(String methodKey, Object o, MethodProxy methodProxy, Object[] args) throws Throwable {
        ExecutorService executor = circuitBreakerManager.getExecutor(methodKey);
        Future<Object> invokeFuture = executor.submit(() -> {
            try {
                return methodProxy.invoke(o, args);
            } catch (Throwable e) {
                throw new ExecuteTaskException(e);
            }
        });

        try {
            return invokeFuture.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            invokeFuture.cancel(true);
            log.error("hystrix proxy invokeTargetMethodByThreadPool error", e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            invokeFuture.cancel(true);
            throw e;
        } catch (ExecuteTaskException e) {
            throw e.getCause();
        }

    }


    private boolean allowRequest(String methodKey) {
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(methodKey);
        return circuitBreaker.allowRequest();
    }


    private static Method findFallbackMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes)  {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return findFallbackMethod(superclass, methodName, parameterTypes);
            }
            throw new RuntimeException("fallback method not found");
        }
    }

    private void recordSuccess(String methodKey, long startTime, long endTime) {
        InvokeInfo invokeInfo = InvokeInfo.builder()
                .methodKey(methodKey)
                .success(true)
                .startTime(startTime)
                .duration(endTime - startTime)
                .build();
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(methodKey);
        circuitBreaker.recordSuccess(invokeInfo);
    }

    private void recordFailed(String methodKey, long startInvokeTime, String exceptionName, String exceptionMessage) {
        InvokeInfo invokeInfo = InvokeInfo.builder()
                .methodKey(methodKey)
                .success(false)
                .startTime(startInvokeTime)
                .exceptionName(exceptionName)
                .exceptionMessage(exceptionMessage)
                .build();
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(methodKey);
        circuitBreaker.recordFailed(invokeInfo);
    }
}
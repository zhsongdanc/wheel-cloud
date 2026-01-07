package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.anno.RateLimit;
import com.wheel.cloud.hystrix.enums.IsolationTypeEnum;
import com.wheel.cloud.hystrix.exception.ForbiddenRequestException;
import com.wheel.cloud.hystrix.exception.RejectExecuteException;
import com.wheel.cloud.hystrix.exception.SemaphoreExceedLimitException;
import com.wheel.cloud.hystrix.invoke.InvokeStrategy;
import com.wheel.cloud.hystrix.invoke.InvokeStrategyFactory;
import com.wheel.cloud.hystrix.limit.RateLimiter;
import com.wheel.cloud.hystrix.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

@Slf4j
public class CompositeInterceptor implements MethodInterceptor {

    private CircuitBreakerManager circuitBreakerManager;

    private RateLimiterManager rateLimiterManager;

    public CompositeInterceptor(CircuitBreakerManager circuitBreakerManager, RateLimiterManager rateLimiterManager) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.rateLimiterManager = rateLimiterManager;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (method.isAnnotationPresent(RateLimit.class)) {
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            int algorithm = rateLimit.algorithm();
            int permits = rateLimit.permitsPerSecond();
            String methodKey = ClassUtil.generateMethodKey(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes());

            RateLimiter rateLimiter = rateLimiterManager.getRateLimiter(methodKey, algorithm, permits);
            if (!rateLimiter.allowRequest()) {
                throw new RejectExecuteException("Rate limit exceeded");
            }
        }

        if (method.isAnnotationPresent(HystrixCommand.class)) {
            log.info("hystrix proxy intercept,className:{},methodName:{}", o.getClass().getName(), method.getName());
            HystrixCommand annotation = method.getAnnotation(HystrixCommand.class);
            String fallbackMethodName = annotation.fallbackMethod();
            String methodKey = annotation.methodKey();
            String groupKey = annotation.groupKey();
            IsolationTypeEnum isolation = annotation.isolation();
            InvokeStrategy invokeStrategy = InvokeStrategyFactory.getInvokeStrategy(isolation, circuitBreakerManager);

            if (StringUtils.isBlank(methodKey)) {
                methodKey = ClassUtil.generateMethodKey(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes());
            }
            if (StringUtils.isBlank(groupKey)) {
                groupKey = method.getDeclaringClass().getName();
            }

            Object res = null;
            long startInvokeTime = System.currentTimeMillis();
            try {
                if (!allowRequest(methodKey)) {
                    throw new ForbiddenRequestException("circuit breaker is open");
                }
                res = invokeStrategy.invoke(groupKey, o, methodProxy, args, annotation);
                recordSuccess(methodKey, startInvokeTime, System.currentTimeMillis());
            } catch (Throwable throwable) {
                log.error("hystrix proxy fallbackMethod invoke error, but use default method");
                if (StringUtils.isNotBlank(fallbackMethodName)) {
                    Method fallbackMethod = findFallbackMethod(method.getDeclaringClass(), fallbackMethodName, method.getParameterTypes());
                    fallbackMethod.setAccessible(true);
                    try {
                        res = fallbackMethod.invoke(o, args);
                    } catch (Exception fallbackException) {
                        throw new ForbiddenRequestException("fallback method invoke error", fallbackException);
                    }
                }
                if (throwable instanceof ForbiddenRequestException || throwable instanceof SemaphoreExceedLimitException) {
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
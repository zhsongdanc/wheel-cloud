package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.awt.*;
import java.lang.reflect.Method;

@Slf4j
public class HystrixMethodInterceptor implements MethodInterceptor {

    private CircuitBreakerManager circuitBreakerManager;

    public HystrixMethodInterceptor(CircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        if (method.isAnnotationPresent(HystrixCommand.class)) {
            log.info("hystrix proxy intercept,className:{},methodName:{}", o.getClass().getName(), method.getName());
            HystrixCommand annotation = method.getAnnotation(HystrixCommand.class);
            String fallbackMethodName = annotation.fallbackMethod();

            String methodKey = ClassUtil.generateMethodKey(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes());

            Object res = null;
            long startInvokeTime = System.currentTimeMillis();
            try {
                if (!allowRequest(methodKey)) {
                    throw new FontFormatException("circuit breaker is open");
                }
                res = methodProxy.invokeSuper(o, objects);
                recordSuccess(methodKey, startInvokeTime, System.currentTimeMillis());
            } catch (Exception e) {
                log.error("hystrix proxy fallbackMethod invoke error, but use defalut method");
                if (StringUtils.isNotBlank(fallbackMethodName)) {
                    Method fallbackMethod = findFallbackMethod(method.getDeclaringClass(), fallbackMethodName, method.getParameterTypes());
                    fallbackMethod.setAccessible(true);
                    res = fallbackMethod.invoke(o, objects);
                }
                recordFailed(methodKey, e.getClass().getName(), e.getMessage());
            }

            log.info("hystrix proxy intercept end,className:{},methodName:{}", o.getClass().getName(), method.getName());
            return res;
        }

        return methodProxy.invokeSuper(o, objects);
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
                .duration(endTime - startTime)
                .build();
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(methodKey);
        circuitBreaker.recordSuccess(invokeInfo);
    }

    private void recordFailed(String methodKey, String exceptionName, String exceptionMessage) {
        InvokeInfo invokeInfo = InvokeInfo.builder()
                .methodKey(methodKey)
                .success(false)
                .exceptionName(exceptionName)
                .exceptionMessage(exceptionMessage)
                .build();
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(methodKey);
        circuitBreaker.recordFailed(invokeInfo);
    }
}
package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.anno.RateLimit;
import com.wheel.cloud.hystrix.exception.RejectExecuteException;
import com.wheel.cloud.hystrix.limit.RateLimiter;
import com.wheel.cloud.hystrix.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

@Slf4j
public class RateLimitInterceptor implements MethodInterceptor {

    private final RateLimiterManager rateLimiterManager;

    public RateLimitInterceptor(RateLimiterManager rateLimiterManager) {
        this.rateLimiterManager = rateLimiterManager;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

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

        return methodProxy.invokeSuper(o, objects);
    }
}

package com.wheel.cloud.hystrix.spring;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.anno.RateLimit;
import com.wheel.cloud.hystrix.config.CircuitBreakerManager;
import com.wheel.cloud.hystrix.config.CompositeInterceptor;
import com.wheel.cloud.hystrix.config.RateLimiterManager;
import com.wheel.cloud.hystrix.property.CommandProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Component
@Slf4j
public class HystrixPostProcessor implements BeanPostProcessor {

    @Resource
    private CircuitBreakerManager circuitBreakerManager;

    @Resource
    private RateLimiterManager rateLimiterManager;

    @Resource
    private CommandProperty commandProperty;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(HystrixCommand.class) || method.isAnnotationPresent(RateLimit.class)) {

                log.info("hystrix proxy create,className:{},methodName:{}", beanClass.getName(), method.getName());
                return createProxyObject(bean);
            } else {
                log.info("postProcessAfterInitialization normal");
            }
        }
        return bean;
    }

    private Object createProxyObject(Object originObj) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(originObj.getClass());
        enhancer.setCallback(new CompositeInterceptor(circuitBreakerManager, rateLimiterManager));
        return enhancer.create();
    }




}

package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Component
@Slf4j
public class HystrixPostProcessor implements BeanPostProcessor {


    @Resource
    private CircuitBreakerManager circuitBreakerManager;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(HystrixCommand.class)) {
                HystrixCommand hystrixCommand = method.getAnnotation(HystrixCommand.class);
                String fallbackMethod = hystrixCommand.fallbackMethod();
                if (StringUtils.isNotBlank(fallbackMethod)) {
                    log.info("hystrix proxy create,className:{},methodName:{}", beanClass.getName(), method.getName());
                    return createProxyObject(bean);
                } else {
                    log.info("postProcessAfterInitialization normal");
                }
            }
        }
        return bean;
    }

    private Object createProxyObject(Object originObj) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(originObj.getClass());
        enhancer.setCallback(new HystrixMethodInterceptor(circuitBreakerManager));
        return enhancer.create();
    }




}

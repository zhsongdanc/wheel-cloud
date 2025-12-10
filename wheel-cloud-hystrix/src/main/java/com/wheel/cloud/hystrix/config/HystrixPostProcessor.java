package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class HystrixPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
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
                    log.warn("beanName:{}, hystrix fallbackMethod: {}", beanName, fallbackMethod);
                }
            }
        }


        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}

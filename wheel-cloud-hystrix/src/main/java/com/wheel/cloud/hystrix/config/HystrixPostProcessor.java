package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Component
@Slf4j
public class HystrixPostProcessor implements BeanPostProcessor {

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
        enhancer.setCallback(new HystrixMethodInterceptor());
        return enhancer.create();
    }

    public static class HystrixMethodInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            if (method.isAnnotationPresent(HystrixCommand.class)) {
                log.info("hystrix proxy intercept,className:{},methodName:{}", o.getClass().getName(), method.getName());
                HystrixCommand annotation = method.getAnnotation(HystrixCommand.class);
                String fallbackMethodName = annotation.fallbackMethod();


                Object res = null;
                try {
                    res = methodProxy.invokeSuper(o, objects);
                } catch (Exception e) {
                    log.error("hystrix proxy fallbackMethod invoke error, but use defalut method");
                    if (StringUtils.isNotBlank(fallbackMethodName)) {
                        Method fallbackMethod = findFallbackMethod(method.getDeclaringClass(), fallbackMethodName, method.getParameterTypes());
                        fallbackMethod.setAccessible(true);
                        res = fallbackMethod.invoke(o, objects);
                    }
                }

                log.info("hystrix proxy intercept end,className:{},methodName:{}", o.getClass().getName(), method.getName());
                return res;
            }

            return methodProxy.invokeSuper(o, objects);
        }
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
}

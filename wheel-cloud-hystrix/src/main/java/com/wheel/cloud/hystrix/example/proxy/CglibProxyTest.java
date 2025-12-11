package com.wheel.cloud.hystrix.example.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

public class CglibProxyTest {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserService.class);
        enhancer.setCallback(new UserServiceInterceptor());
        UserService userService = (UserService) enhancer.create();
        // 代理类
        userService.doSomething("szh");
    }



    @Service
    @Slf4j
    static class UserService {
        public void sayHello() {
            log.info("hello");
        }

        public void doSomething() {
            log.info("doSomething");
        }

        private void doSomething(String name) {
            log.info("doSomething {}", name);
        }

    }

    @Slf4j
    static class UserServiceInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            log.info("before method execution");
            Object result = methodProxy.invokeSuper(o, objects);
            log.info("after method execution");
            return result;
        }

    }


}

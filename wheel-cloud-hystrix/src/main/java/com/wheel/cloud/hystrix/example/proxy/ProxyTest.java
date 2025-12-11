package com.wheel.cloud.hystrix.example.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyTest {


    public static void main(String[] args) {
        Object proxyInstance = Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[]{MyInterface.class},
                new MyInvocationHandler(new MyInterfaceImpl()));
        MyInterface myInterface = (MyInterface) proxyInstance;
        myInterface.doSomething();
    }



    public static class MyInvocationHandler implements InvocationHandler {

        private Object target;

        public MyInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("Before method execution");
            Object result = method.invoke(target, args);
            System.out.println("After method execution");
            return result;
        }
    }


    public interface MyInterface {
        void doSomething();
        void doSomething2();
    }

    public static class MyInterfaceImpl implements MyInterface {

        @Override
        public void doSomething() {
            System.out.println("Doing something");
        }

        @Override
        public void doSomething2() {
            System.out.println("Doing something2");
        }

    }
}

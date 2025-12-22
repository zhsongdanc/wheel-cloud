package com.wheel.cloud.hystrix.invoke;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.enums.IsolationTypeEnum;
import org.springframework.cglib.proxy.MethodProxy;

public interface InvokeStrategy {

    public Object invoke(String groupKey, Object o, MethodProxy methodProxy, Object[] args, HystrixCommand annotation) throws Throwable;

    public IsolationTypeEnum getIsolation();
}

package com.wheel.cloud.loadbalancer.core.rule;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;

import java.util.List;

public interface LoadBalanceRule {

    String name();

    ServiceInstance choose(String serviceName, List<ServiceInstance> instances);
}

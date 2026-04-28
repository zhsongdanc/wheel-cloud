package com.wheel.cloud.loadbalancer.core;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;

import java.util.List;

public interface LoadBalancer {

    ServiceInstance choose(String serviceName, String ruleName);

    List<ServiceInstance> getAvailableInstances(String serviceName);

    void recordFailure(String serviceName, String instanceId);
}

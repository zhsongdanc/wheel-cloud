package com.wheel.cloud.loadbalancer.core.supplier;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;

import java.util.List;

public interface ServiceInstanceSupplier {

    List<ServiceInstance> getInstances(String serviceName);
}

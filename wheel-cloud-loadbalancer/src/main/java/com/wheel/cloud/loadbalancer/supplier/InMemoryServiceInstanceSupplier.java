package com.wheel.cloud.loadbalancer.supplier;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import com.wheel.cloud.loadbalancer.core.supplier.ServiceInstanceSupplier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryServiceInstanceSupplier implements ServiceInstanceSupplier {

    private final ConcurrentMap<String, List<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 第一版先用内存实例列表，后面如果要接 Eureka，本类就是最自然的替换点。
        serviceInstanceMap.put("provider-demo", Arrays.asList(
                new ServiceInstance("provider-demo", "provider-demo-1", "127.0.0.1", 8081, 5, Collections.singletonMap("zone", "A")),
                new ServiceInstance("provider-demo", "provider-demo-2", "127.0.0.1", 8083, 3, Collections.singletonMap("zone", "A")),
                new ServiceInstance("provider-demo", "provider-demo-3", "127.0.0.1", 8084, 1, Collections.singletonMap("zone", "B"))
        ));
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        return serviceInstanceMap.getOrDefault(serviceName, Collections.emptyList());
    }
}

package com.wheel.cloud.loadbalancer.rule;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import com.wheel.cloud.loadbalancer.core.rule.LoadBalanceRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinRule implements LoadBalanceRule {

    private final ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "round-robin";
    }

    @Override
    public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        AtomicInteger counter = counters.computeIfAbsent(serviceName, key -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement());
        return instances.get(index % instances.size());
    }
}

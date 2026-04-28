package com.wheel.cloud.loadbalancer.rule;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import com.wheel.cloud.loadbalancer.core.rule.LoadBalanceRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class WeightedRandomRule implements LoadBalanceRule {

    @Override
    public String name() {
        return "weighted-random";
    }

    @Override
    public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (ServiceInstance instance : instances) {
            totalWeight += Math.max(instance.getWeight(), 1);
        }
        int offset = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (ServiceInstance instance : instances) {
            current += Math.max(instance.getWeight(), 1);
            if (offset < current) {
                return instance;
            }
        }
        return instances.get(instances.size() - 1);
    }
}

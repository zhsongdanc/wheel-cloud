package com.wheel.cloud.loadbalancer.rule;

import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import com.wheel.cloud.loadbalancer.core.rule.LoadBalanceRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomRule implements LoadBalanceRule {

    @Override
    public String name() {
        return "random";
    }

    @Override
    public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }
}

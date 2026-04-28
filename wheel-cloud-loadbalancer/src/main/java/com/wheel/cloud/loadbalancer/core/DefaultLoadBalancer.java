package com.wheel.cloud.loadbalancer.core;

import com.wheel.cloud.loadbalancer.core.failover.FailureRecord;
import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import com.wheel.cloud.loadbalancer.core.rule.LoadBalanceRule;
import com.wheel.cloud.loadbalancer.core.supplier.ServiceInstanceSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DefaultLoadBalancer implements LoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(DefaultLoadBalancer.class);

    private final ServiceInstanceSupplier serviceInstanceSupplier;

    private final Map<String, LoadBalanceRule> ruleMap;

    private final Map<String, FailureRecord> failureRecordMap = new ConcurrentHashMap<>();

    public DefaultLoadBalancer(ServiceInstanceSupplier serviceInstanceSupplier, List<LoadBalanceRule> rules) {
        this.serviceInstanceSupplier = serviceInstanceSupplier;
        this.ruleMap = rules.stream().collect(Collectors.toMap(LoadBalanceRule::name, rule -> rule));
    }

    @Override
    public ServiceInstance choose(String serviceName, String ruleName) {
        List<ServiceInstance> availableInstances = getAvailableInstances(serviceName);
        LoadBalanceRule rule = ruleMap.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("unsupported load balance rule: " + ruleName);
        }
        ServiceInstance chosen = rule.choose(serviceName, availableInstances);
        if (chosen != null) {
            log.info("load balancer chose instance: serviceName={}, rule={}, instanceId={}, host={}, port={}",
                    serviceName, ruleName, chosen.getInstanceId(), chosen.getHost(), chosen.getPort());
        }
        return chosen;
    }

    @Override
    public List<ServiceInstance> getAvailableInstances(String serviceName) {
        long now = System.currentTimeMillis();
        List<ServiceInstance> instances = serviceInstanceSupplier.getInstances(serviceName);
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }
        // 这里模拟客户端侧“短暂失败实例剔除”，体现发现和治理之间的中间层角色。
        List<ServiceInstance> availableInstances = instances.stream()
                .filter(instance -> isAvailable(instance, now))
                .collect(Collectors.toList());
        log.info("load balancer loaded instances: serviceName={}, total={}, available={}",
                serviceName, instances.size(), availableInstances.size());
        return availableInstances;
    }

    @Override
    public void recordFailure(String serviceName, String instanceId) {
        long failUntil = System.currentTimeMillis() + 10_000L;
        failureRecordMap.put(buildFailureKey(serviceName, instanceId), new FailureRecord(failUntil));
        log.warn("load balancer marked instance as temporarily unavailable: serviceName={}, instanceId={}, failUntil={}",
                serviceName, instanceId, failUntil);
    }

    private boolean isAvailable(ServiceInstance instance, long now) {
        FailureRecord failureRecord = failureRecordMap.get(buildFailureKey(instance.getServiceName(), instance.getInstanceId()));
        return failureRecord == null || failureRecord.available(now);
    }

    private String buildFailureKey(String serviceName, String instanceId) {
        return serviceName + "#" + instanceId;
    }
}

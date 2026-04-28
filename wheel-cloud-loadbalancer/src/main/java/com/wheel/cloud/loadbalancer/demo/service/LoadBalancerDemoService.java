package com.wheel.cloud.loadbalancer.demo.service;

import com.wheel.cloud.loadbalancer.core.LoadBalancer;
import com.wheel.cloud.loadbalancer.core.model.ServiceInstance;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoadBalancerDemoService {

    private final LoadBalancer loadBalancer;

    public LoadBalancerDemoService(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public Map<String, Object> choose(String serviceName, String ruleName) {
        ServiceInstance chosen = loadBalancer.choose(serviceName, ruleName);
        if (chosen == null) {
            throw new IllegalStateException("no available instance for serviceName=" + serviceName);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName);
        response.put("rule", ruleName);
        response.put("instanceId", chosen.getInstanceId());
        response.put("host", chosen.getHost());
        response.put("port", chosen.getPort());
        response.put("weight", chosen.getWeight());
        return response;
    }

    public Map<String, Object> list(String serviceName) {
        List<ServiceInstance> instances = loadBalancer.getAvailableInstances(serviceName);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName);
        response.put("instanceCount", instances.size());
        response.put("instances", instances.stream().map(instance -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("instanceId", instance.getInstanceId());
            view.put("host", instance.getHost());
            view.put("port", instance.getPort());
            view.put("weight", instance.getWeight());
            return view;
        }).collect(Collectors.toList()));
        return response;
    }

    public Map<String, Object> fail(String serviceName, String instanceId) {
        loadBalancer.recordFailure(serviceName, instanceId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName);
        response.put("instanceId", instanceId);
        response.put("message", "instance temporarily marked unavailable");
        return response;
    }
}

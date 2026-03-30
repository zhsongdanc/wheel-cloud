package com.wheel.cloud.eureka.server.registry;

import com.wheel.cloud.eureka.server.RegistryProperties;
import com.wheel.cloud.eureka.server.model.InstanceInfo;
import com.wheel.cloud.eureka.server.model.Lease;
import com.wheel.cloud.eureka.server.model.LeaseView;
import com.wheel.cloud.eureka.server.model.RegisterInstanceRequest;
import com.wheel.cloud.eureka.server.model.RegistryInstanceView;
import com.wheel.cloud.eureka.server.model.RegistrySnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class InMemoryRegistry {

    private final RegistryProperties registryProperties;

    // 注册中心维护的不是简单实例列表，而是 service -> instance -> lease 的关系。
    private final ConcurrentMap<String, ConcurrentMap<String, Lease<InstanceInfo>>> registry = new ConcurrentHashMap<>();

    public InMemoryRegistry(RegistryProperties registryProperties) {
        this.registryProperties = registryProperties;
    }

    public Lease<InstanceInfo> register(RegisterInstanceRequest request) {
        validateRequest(request);
        // InstanceInfo 表达“实例是谁”，Lease 表达“它最近一次证明自己活着是什么时候”。
        InstanceInfo instanceInfo = new InstanceInfo(
                normalizeServiceName(request.getServiceName()),
                request.getInstanceId(),
                request.getHost(),
                request.getPort() == null ? registryProperties.getDefaultServicePort() : request.getPort(),
                request.getStatus(),
                request.getMetadata()
        );
        long now = System.currentTimeMillis();
        Lease<InstanceInfo> lease = new Lease<>(instanceInfo, registryProperties.getLeaseDurationMs(), now);
        // 同一个 serviceName 下允许多个实例并存，因此这里先按服务分组，再按 instanceId 定位。
        registry.computeIfAbsent(instanceInfo.getServiceName(), key -> new ConcurrentHashMap<>())
                .put(instanceInfo.getInstanceId(), lease);
        return lease;
    }

    public boolean renew(String serviceName, String instanceId) {
        Lease<InstanceInfo> lease = getLease(serviceName, instanceId);
        if (lease == null) {
            return false;
        }
        // 续约的本质是更新时间戳，而不是重新注册整个实例。
        lease.renew(System.currentTimeMillis());
        return true;
    }

    public boolean unregister(String serviceName, String instanceId) {
        String normalizedServiceName = normalizeServiceName(serviceName);
        ConcurrentMap<String, Lease<InstanceInfo>> instances = registry.get(normalizedServiceName);
        if (instances == null) {
            return false;
        }
        Lease<InstanceInfo> removed = instances.remove(instanceId);
        if (instances.isEmpty()) {
            registry.remove(normalizedServiceName, instances);
        }
        return removed != null;
    }

    public List<RegistryInstanceView> getInstances(String serviceName) {
        String normalizedServiceName = normalizeServiceName(serviceName);
        ConcurrentMap<String, Lease<InstanceInfo>> instances = registry.get(normalizedServiceName);
        if (instances == null) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        // 读路径把 lease 转成可观察视图，便于在接口层直接看到过期状态。
        return instances.values().stream()
                .map(lease -> toView(lease, now))
                .collect(Collectors.toList());
    }

    public RegistrySnapshot snapshot() {
        long now = System.currentTimeMillis();
        Map<String, List<RegistryInstanceView>> services = registry.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream()
                                .map(lease -> toView(lease, now))
                                .collect(Collectors.toList())
                ));
        int instanceCount = services.values().stream().mapToInt(List::size).sum();
        return new RegistrySnapshot(services.size(), instanceCount, services);
    }

    public List<RegistryInstanceView> evictExpiredInstances() {
        long now = System.currentTimeMillis();
        List<RegistryInstanceView> evicted = new ArrayList<>();
        // 后台定时扫描承担兜底清理职责，解决实例宕机但来不及主动注销的问题。
        registry.forEach((serviceName, instances) -> {
            instances.forEach((instanceId, lease) -> {
                if (lease.isExpired(now) && instances.remove(instanceId, lease)) {
                    evicted.add(toView(lease, now));
                }
            });
            if (instances.isEmpty()) {
                registry.remove(serviceName, instances);
            }
        });
        return evicted;
    }

    private Lease<InstanceInfo> getLease(String serviceName, String instanceId) {
        ConcurrentMap<String, Lease<InstanceInfo>> instances = registry.get(normalizeServiceName(serviceName));
        if (instances == null) {
            return null;
        }
        return instances.get(instanceId);
    }

    private RegistryInstanceView toView(Lease<InstanceInfo> lease, long now) {
        return new RegistryInstanceView(
                lease.getHolder(),
                new LeaseView(
                        lease.getRegistrationTimestamp(),
                        lease.getLastRenewalTimestamp(),
                        lease.getDurationMs(),
                        lease.isExpired(now)
                )
        );
    }

    private void validateRequest(RegisterInstanceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!StringUtils.hasText(request.getServiceName())) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        if (!StringUtils.hasText(request.getInstanceId())) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        if (!StringUtils.hasText(request.getHost())) {
            throw new IllegalArgumentException("host must not be blank");
        }
    }

    private String normalizeServiceName(String serviceName) {
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        return serviceName.trim().toUpperCase();
    }
}

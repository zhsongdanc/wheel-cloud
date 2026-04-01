package com.wheel.cloud.eureka.registry.store;

import com.wheel.cloud.eureka.registry.config.RegistryProperties;
import com.wheel.cloud.eureka.registry.model.DeltaEventType;
import com.wheel.cloud.eureka.registry.model.InstanceInfo;
import com.wheel.cloud.eureka.registry.model.Lease;
import com.wheel.cloud.eureka.registry.model.LeaseView;
import com.wheel.cloud.eureka.registry.model.RegisterInstanceRequest;
import com.wheel.cloud.eureka.registry.model.RegistryInstanceView;
import com.wheel.cloud.eureka.registry.model.RegistrySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InMemoryRegistry.class);

    private final RegistryProperties registryProperties;
    private final RegistryDeltaLog registryDeltaLog;

    // 注册中心维护的是 service -> instance -> lease，而不是平铺实例表。
    private final ConcurrentMap<String, ConcurrentMap<String, Lease<InstanceInfo>>> registry = new ConcurrentHashMap<>();

    public InMemoryRegistry(RegistryProperties registryProperties, RegistryDeltaLog registryDeltaLog) {
        this.registryProperties = registryProperties;
        this.registryDeltaLog = registryDeltaLog;
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
        registry.computeIfAbsent(instanceInfo.getServiceName(), key -> new ConcurrentHashMap<>())
                .put(instanceInfo.getInstanceId(), lease);
        registryDeltaLog.append(DeltaEventType.REGISTER, instanceInfo);
        log.info("registered instance: serviceName={}, instanceId={}, host={}, port={}",
                instanceInfo.getServiceName(), instanceInfo.getInstanceId(), instanceInfo.getHost(), instanceInfo.getPort());
        return lease;
    }

    public boolean renew(String serviceName, String instanceId) {
        Lease<InstanceInfo> lease = getLease(serviceName, instanceId);
        if (lease == null) {
            return false;
        }
        // 续约只更新时间，不重建实例。
        lease.renew(System.currentTimeMillis());
        log.debug("renewed lease: serviceName={}, instanceId={}", normalizeServiceName(serviceName), instanceId);
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
        if (removed != null) {
            registryDeltaLog.append(DeltaEventType.UNREGISTER, removed.getHolder());
            log.info("unregistered instance: serviceName={}, instanceId={}", normalizedServiceName, instanceId);
        }
        return removed != null;
    }

    public List<RegistryInstanceView> getInstances(String serviceName) {
        ConcurrentMap<String, Lease<InstanceInfo>> instances = registry.get(normalizeServiceName(serviceName));
        if (instances == null) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
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
        // 后台扫描负责脏数据清理，避免把清理逻辑塞进注册/查询主链路。
        registry.forEach((serviceName, instances) -> {
            instances.forEach((instanceId, lease) -> {
                if (lease.isExpired(now) && instances.remove(instanceId, lease)) {
                    evicted.add(toView(lease, now));
                    registryDeltaLog.append(DeltaEventType.EXPIRE, lease.getHolder());
                    log.warn("evicted expired instance: serviceName={}, instanceId={}, lastRenewalTimestamp={}",
                            serviceName, instanceId, lease.getLastRenewalTimestamp());
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

package com.wheel.cloud.eureka.consumer.client;

import com.wheel.cloud.eureka.consumer.client.model.ClientCacheView;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistryInstanceView;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistrySnapshot;
import com.wheel.cloud.eureka.consumer.client.model.ClientLeaseView;
import com.wheel.cloud.eureka.consumer.client.model.DeltaEventType;
import com.wheel.cloud.eureka.consumer.client.model.DeltaSyncResponse;
import com.wheel.cloud.eureka.consumer.client.model.RegistryDeltaEvent;
import com.wheel.cloud.eureka.consumer.client.model.RegisterInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class EurekaLikeClient {

    private static final Logger log = LoggerFactory.getLogger(EurekaLikeClient.class);

    private final DiscoveryClientProperties properties;
    private final RestTemplate restTemplate;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicLong lastFetchTimestamp = new AtomicLong(0L);
    private final AtomicLong lastSeenVersion = new AtomicLong(0L);
    private final AtomicReference<ClientRegistrySnapshot> localCache = new AtomicReference<>(emptySnapshot());

    public EurekaLikeClient(DiscoveryClientProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("consumer startup: serviceName={}, instanceId={}, host={}, port={}, registryUrl={}",
                properties.getServiceName(), properties.getInstanceId(), properties.getHost(),
                properties.getPort(), properties.getRegistryUrl());
        registerIfNecessary();
        fetchRegistry();
    }

    @Scheduled(fixedDelayString = "${discovery.client.renewal-interval-ms:5000}")
    public void renewLease() {
        if (!registered.get()) {
            registerIfNecessary();
            return;
        }
        try {
            restTemplate.put(buildInstancePath(), null);
            log.info("consumer lease renewed: serviceName={}, instanceId={}",
                    properties.getServiceName(), properties.getInstanceId());
        } catch (RestClientException exception) {
            registered.set(false);
            log.warn("failed to renew consumer lease, will retry register", exception);
        }
    }

    @Scheduled(fixedDelayString = "${discovery.client.fetch-registry-interval-ms:8000}")
    public void fetchRegistry() {
        try {
            DeltaSyncResponse delta = restTemplate.getForObject(buildDeltaPath(), DeltaSyncResponse.class, lastSeenVersion.get());
            if (delta != null) {
                if (delta.isFullSnapshotRequired()) {
                    log.warn("consumer delta sync requires full snapshot: lastSeenVersion={}, targetVersion={}",
                            lastSeenVersion.get(), delta.getToVersion());
                    refreshFullSnapshot(delta.getToVersion());
                    return;
                }
                applyDelta(delta);
            }
        } catch (RestClientException exception) {
            log.warn("failed to fetch registry snapshot, will continue using local cache", exception);
        }
    }

    public List<ClientRegistryInstanceView> getInstances(String serviceName) {
        Map<String, List<ClientRegistryInstanceView>> services = localCache.get().getServices();
        if (services == null) {
            return Collections.emptyList();
        }
        List<ClientRegistryInstanceView> instances = services.get(serviceName.trim().toUpperCase(Locale.ROOT));
        return instances == null ? Collections.emptyList() : instances;
    }

    public ClientRegistryInstanceView chooseFirstInstance(String serviceName) {
        List<ClientRegistryInstanceView> candidates = getAvailableInstances(serviceName);
        ClientRegistryInstanceView chosen = candidates.stream()
                .min(Comparator.comparing(instance -> instance.getInstanceInfo().getInstanceId()))
                .orElse(null);
        if (chosen != null) {
            log.info("consumer chose instance from local cache: serviceName={}, instanceId={}, host={}, port={}",
                    serviceName, chosen.getInstanceInfo().getInstanceId(),
                    chosen.getInstanceInfo().getHost(), chosen.getInstanceInfo().getPort());
        } else {
            log.warn("consumer found no available instance in local cache: serviceName={}", serviceName);
        }
        return chosen;
    }

    public List<ClientRegistryInstanceView> getAvailableInstances(String serviceName) {
        List<ClientRegistryInstanceView> candidates = getInstances(serviceName).stream()
                .filter(instance -> instance.getLease() == null || !instance.getLease().isExpired())
                .sorted(Comparator.comparing(instance -> instance.getInstanceInfo().getInstanceId()))
                .collect(Collectors.toList());
        log.info("consumer loaded available instances from local cache: serviceName={}, candidateCount={}",
                serviceName, candidates.size());
        return candidates;
    }

    public ClientCacheView getCacheView() {
        return new ClientCacheView(registered.get(), lastFetchTimestamp.get(), lastSeenVersion.get(), localCache.get());
    }

    public long forceLastSeenVersion(long version) {
        long previousVersion = lastSeenVersion.getAndSet(version);
        log.warn("consumer force updated lastSeenVersion for debugging: previousVersion={}, newVersion={}",
                previousVersion, version);
        return previousVersion;
    }

    private void registerIfNecessary() {
        if (registered.get()) {
            return;
        }
        try {
            RegisterInstanceRequest request = new RegisterInstanceRequest();
            request.setServiceName(properties.getServiceName());
            request.setInstanceId(properties.getInstanceId());
            request.setHost(properties.getHost());
            request.setPort(properties.getPort());
            restTemplate.postForObject(buildAppsPath(), request, Object.class);
            registered.set(true);
            log.info("consumer registered to registry: serviceName={}, instanceId={}, host={}, port={}",
                    properties.getServiceName(), properties.getInstanceId(), properties.getHost(), properties.getPort());
        } catch (RestClientException exception) {
            log.warn("failed to register consumer to registry", exception);
        }
    }

    private String buildAppsPath() {
        return normalizeRegistryUrl() + "/registry/apps";
    }

    private String buildDeltaPath() {
        return normalizeRegistryUrl() + "/registry/delta?lastSeenVersion={lastSeenVersion}";
    }

    private String buildInstancePath() {
        return buildAppsPath() + "/" + properties.getServiceName().trim().toUpperCase(Locale.ROOT) + "/" + properties.getInstanceId();
    }

    private String normalizeRegistryUrl() {
        String registryUrl = properties.getRegistryUrl();
        return registryUrl.endsWith("/") ? registryUrl.substring(0, registryUrl.length() - 1) : registryUrl;
    }

    private static ClientRegistrySnapshot emptySnapshot() {
        ClientRegistrySnapshot snapshot = new ClientRegistrySnapshot();
        snapshot.setServiceCount(0);
        snapshot.setInstanceCount(0);
        snapshot.setServices(Collections.emptyMap());
        return snapshot;
    }

    private void refreshFullSnapshot(long targetVersion) {
        // 全量同步解决的是“纠偏”问题：当增量基础不可信时，重新建立一份完整快照。
        ClientRegistrySnapshot snapshot = restTemplate.getForObject(buildAppsPath(), ClientRegistrySnapshot.class);
        if (snapshot != null) {
            localCache.set(snapshot);
            lastSeenVersion.set(targetVersion);
            lastFetchTimestamp.set(System.currentTimeMillis());
            log.info("consumer refreshed full snapshot: serviceCount={}, instanceCount={}, targetVersion={}",
                    snapshot.getServiceCount(), snapshot.getInstanceCount(), targetVersion);
        }
    }

    private void applyDelta(DeltaSyncResponse delta) {
        if (delta.getEvents() == null || delta.getEvents().isEmpty()) {
            lastSeenVersion.set(delta.getToVersion());
            lastFetchTimestamp.set(System.currentTimeMillis());
            log.info("consumer delta sync found no new events: lastSeenVersion={}", delta.getToVersion());
            return;
        }

        ClientRegistrySnapshot newSnapshot = copySnapshot(localCache.get());
        Map<String, List<ClientRegistryInstanceView>> services = newSnapshot.getServices();
        for (RegistryDeltaEvent event : delta.getEvents()) {
            applyEvent(services, event);
        }
        updateSnapshotCounters(newSnapshot);
        localCache.set(newSnapshot);
        lastSeenVersion.set(delta.getToVersion());
        lastFetchTimestamp.set(System.currentTimeMillis());
        log.info("consumer applied delta events: eventCount={}, fromVersion={}, toVersion={}",
                delta.getEvents().size(), delta.getFromVersion(), delta.getToVersion());
    }

    private void applyEvent(Map<String, List<ClientRegistryInstanceView>> services, RegistryDeltaEvent event) {
        if (event.getInstanceInfo() == null) {
            return;
        }
        String serviceName = event.getInstanceInfo().getServiceName();
        List<ClientRegistryInstanceView> currentInstances = services.computeIfAbsent(serviceName, key -> new java.util.ArrayList<>());
        currentInstances.removeIf(instance -> instance.getInstanceInfo().getInstanceId().equals(event.getInstanceInfo().getInstanceId()));

        if (event.getEventType() == DeltaEventType.REGISTER) {
            ClientRegistryInstanceView newInstance = new ClientRegistryInstanceView();
            newInstance.setInstanceInfo(event.getInstanceInfo());
            ClientLeaseView lease = new ClientLeaseView();
            lease.setExpired(false);
            newInstance.setLease(lease);
            currentInstances.add(newInstance);
        }

        if (currentInstances.isEmpty()) {
            services.remove(serviceName);
        }
    }

    private ClientRegistrySnapshot copySnapshot(ClientRegistrySnapshot source) {
        ClientRegistrySnapshot copy = new ClientRegistrySnapshot();
        copy.setServiceCount(source.getServiceCount());
        copy.setInstanceCount(source.getInstanceCount());

        Map<String, List<ClientRegistryInstanceView>> copiedServices = new LinkedHashMap<>();
        if (source.getServices() != null) {
            source.getServices().forEach((serviceName, instances) -> copiedServices.put(
                    serviceName,
                    instances.stream().map(this::copyInstanceView).collect(Collectors.toList())
            ));
        }
        copy.setServices(copiedServices);
        return copy;
    }

    private ClientRegistryInstanceView copyInstanceView(ClientRegistryInstanceView source) {
        ClientRegistryInstanceView copy = new ClientRegistryInstanceView();
        copy.setInstanceInfo(source.getInstanceInfo());
        copy.setLease(source.getLease());
        return copy;
    }

    private void updateSnapshotCounters(ClientRegistrySnapshot snapshot) {
        int serviceCount = snapshot.getServices().size();
        int instanceCount = snapshot.getServices().values().stream().mapToInt(List::size).sum();
        snapshot.setServiceCount(serviceCount);
        snapshot.setInstanceCount(instanceCount);
    }

}

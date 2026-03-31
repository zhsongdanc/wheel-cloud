package com.wheel.cloud.eureka.consumer.client;

import com.wheel.cloud.eureka.consumer.client.model.ClientCacheView;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistryInstanceView;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistrySnapshot;
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
            // 业务线程只读本地缓存；刷新缓存由后台线程完成，避免把注册中心拉进主调用链。
            ClientRegistrySnapshot snapshot = restTemplate.getForObject(buildAppsPath(), ClientRegistrySnapshot.class);
            if (snapshot != null) {
                localCache.set(snapshot);
                lastFetchTimestamp.set(System.currentTimeMillis());
                log.info("consumer fetched registry snapshot: serviceCount={}, instanceCount={}",
                        snapshot.getServiceCount(), snapshot.getInstanceCount());
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
        return new ClientCacheView(registered.get(), lastFetchTimestamp.get(), localCache.get());
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
}

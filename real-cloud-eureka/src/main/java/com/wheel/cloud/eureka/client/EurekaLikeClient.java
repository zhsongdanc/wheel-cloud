package com.wheel.cloud.eureka.client;

import com.wheel.cloud.eureka.client.model.ClientCacheView;
import com.wheel.cloud.eureka.client.model.ClientRegistryInstanceView;
import com.wheel.cloud.eureka.client.model.ClientRegistrySnapshot;
import com.wheel.cloud.eureka.server.model.RegisterInstanceRequest;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public void onApplicationReady() {
        if (!properties.isEnabled()) {
            return;
        }
        registerIfNecessary();
        fetchRegistry();
    }

    @Scheduled(fixedDelayString = "${discovery.client.renewal-interval-ms:5000}")
    public void renewLease() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!registered.get()) {
            registerIfNecessary();
            return;
        }
        try {
            // 心跳是客户端主动证明自己还活着，目的是维持 lease，不是重新注册实例。
            restTemplate.put(buildInstancePath(), null);
        } catch (RestClientException exception) {
            registered.set(false);
            log.warn("failed to renew lease, client will try to re-register on next cycle", exception);
        }
    }

    @Scheduled(fixedDelayString = "${discovery.client.fetch-registry-interval-ms:8000}")
    public void fetchRegistry() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            // 业务线程只读本地缓存；缓存刷新由后台线程完成，避免把注册中心拉进请求主链路。
            ClientRegistrySnapshot snapshot = restTemplate.getForObject(
                    buildAppsPath(),
                    ClientRegistrySnapshot.class
            );
            if (snapshot != null) {
                localCache.set(snapshot);
                lastFetchTimestamp.set(System.currentTimeMillis());
            }
        } catch (RestClientException exception) {
            log.warn("failed to fetch registry snapshot, client will continue using last local cache", exception);
        }
    }

    public List<ClientRegistryInstanceView> getInstances(String serviceName) {
        ClientRegistrySnapshot snapshot = localCache.get();
        Map<String, List<ClientRegistryInstanceView>> services = snapshot.getServices();
        if (services == null) {
            return Collections.emptyList();
        }
        List<ClientRegistryInstanceView> instances = services.get(normalizeServiceName(serviceName));
        return instances == null ? Collections.emptyList() : instances;
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
            // 注册是客户端主动上报自己的身份信息，注册中心只负责接收和维护注册表。
            restTemplate.postForObject(buildAppsPath(), request, Object.class);
            registered.set(true);
        } catch (RestClientException exception) {
            log.warn("failed to register instance to registry server", exception);
        }
    }

    private String buildAppsPath() {
        return normalizeRegistryUrl() + "/registry/apps";
    }

    private String buildInstancePath() {
        return buildAppsPath() + "/" + normalizeServiceName(properties.getServiceName()) + "/" + properties.getInstanceId();
    }

    private String normalizeRegistryUrl() {
        String registryUrl = properties.getRegistryUrl();
        return registryUrl.endsWith("/") ? registryUrl.substring(0, registryUrl.length() - 1) : registryUrl;
    }

    private String normalizeServiceName(String serviceName) {
        return serviceName.trim().toUpperCase(Locale.ROOT);
    }

    private static ClientRegistrySnapshot emptySnapshot() {
        ClientRegistrySnapshot snapshot = new ClientRegistrySnapshot();
        snapshot.setServiceCount(0);
        snapshot.setInstanceCount(0);
        snapshot.setServices(Collections.emptyMap());
        return snapshot;
    }
}

package com.wheel.cloud.eureka.provider.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProviderRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistryClient.class);

    private final ProviderClientProperties properties;
    private final RestTemplate restTemplate;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicBoolean registrationDirty = new AtomicBoolean(true);
    private final AtomicBoolean registrationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean replicatorStarted = new AtomicBoolean(false);

    public ProviderRegistryClient(ProviderClientProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("provider startup: serviceName={}, instanceId={}, host={}, port={}, registryUrl={}",
                properties.getServiceName(), properties.getInstanceId(), properties.getHost(),
                properties.getPort(), properties.getRegistryUrl());
        startReplicator();
    }

    @Scheduled(fixedDelayString = "${discovery.client.renewal-interval-ms:5000}")
    public void renewLease() {
        if (!registered.get()) {
            requestRegistrationUpdate("lease-renew-found-unregistered");
            return;
        }
        try {
            // provider 只负责注册自己和续约，不维护服务发现缓存。
            restTemplate.put(buildInstancePath(), null);
            log.debug("provider lease renewed: serviceName={}, instanceId={}",
                    properties.getServiceName(), properties.getInstanceId());
        } catch (RestClientException exception) {
            registered.set(false);
            registrationDirty.set(true);
            log.warn("failed to renew provider lease, will retry register", exception);
        }
    }

    private void startReplicator() {
        if (!replicatorStarted.compareAndSet(false, true)) {
            return;
        }
        requestRegistrationUpdate("startup");
    }

    private void requestRegistrationUpdate(String reason) {
        registrationDirty.set(true);
        replicateIfNecessary(reason);
    }

    private void replicateIfNecessary(String reason) {
        if (!registrationDirty.get()) {
            return;
        }
        if (!registrationInProgress.compareAndSet(false, true)) {
            log.debug("provider registration update skipped because another replication is in progress: reason={}", reason);
            return;
        }
        try {
            if (!registrationDirty.get()) {
                return;
            }
            RegisterInstanceRequest request = new RegisterInstanceRequest();
            request.setServiceName(properties.getServiceName());
            request.setInstanceId(properties.getInstanceId());
            request.setHost(properties.getHost());
            request.setPort(properties.getPort());
            restTemplate.postForObject(buildAppsPath(), request, Object.class);
            registered.set(true);
            registrationDirty.set(false);
            log.info("provider registered to registry: serviceName={}, instanceId={}, host={}, port={}",
                    properties.getServiceName(), properties.getInstanceId(), properties.getHost(), properties.getPort());
        } catch (RestClientException exception) {
            registered.set(false);
            log.warn("failed to register provider to registry", exception);
        } finally {
            registrationInProgress.set(false);
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
}

package com.wheel.cloud.eureka.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discovery.client")
public class DiscoveryClientProperties {

    private boolean enabled = false;

    private String serviceName = "sample-client";

    private String instanceId = "sample-client-1";

    private String host = "127.0.0.1";

    private int port = 8081;

    private String registryUrl = "http://127.0.0.1:8761";

    private long renewalIntervalMs = 5000L;

    private long fetchRegistryIntervalMs = 8000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public long getRenewalIntervalMs() {
        return renewalIntervalMs;
    }

    public void setRenewalIntervalMs(long renewalIntervalMs) {
        this.renewalIntervalMs = renewalIntervalMs;
    }

    public long getFetchRegistryIntervalMs() {
        return fetchRegistryIntervalMs;
    }

    public void setFetchRegistryIntervalMs(long fetchRegistryIntervalMs) {
        this.fetchRegistryIntervalMs = fetchRegistryIntervalMs;
    }
}

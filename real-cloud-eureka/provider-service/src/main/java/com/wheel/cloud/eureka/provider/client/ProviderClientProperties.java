package com.wheel.cloud.eureka.provider.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discovery.client")
public class ProviderClientProperties {

    private String serviceName = "provider-demo";
    private String instanceId = "provider-demo-1";
    private String host = "127.0.0.1";
    private int port = 8081;
    private String registryUrl = "http://127.0.0.1:8761";
    private long renewalIntervalMs = 5000L;

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
}

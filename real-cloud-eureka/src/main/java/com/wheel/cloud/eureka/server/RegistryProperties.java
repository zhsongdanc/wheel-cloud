package com.wheel.cloud.eureka.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "registry")
public class RegistryProperties {

    private long evictionIntervalMs = 5000L;

    private long leaseDurationMs = 15000L;

    private int defaultServicePort = 8080;

    public long getEvictionIntervalMs() {
        return evictionIntervalMs;
    }

    public void setEvictionIntervalMs(long evictionIntervalMs) {
        this.evictionIntervalMs = evictionIntervalMs;
    }

    public long getLeaseDurationMs() {
        return leaseDurationMs;
    }

    public void setLeaseDurationMs(long leaseDurationMs) {
        this.leaseDurationMs = leaseDurationMs;
    }

    public int getDefaultServicePort() {
        return defaultServicePort;
    }

    public void setDefaultServicePort(int defaultServicePort) {
        this.defaultServicePort = defaultServicePort;
    }
}

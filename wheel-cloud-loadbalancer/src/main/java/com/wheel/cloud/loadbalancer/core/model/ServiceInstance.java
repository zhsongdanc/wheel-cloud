package com.wheel.cloud.loadbalancer.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceInstance {

    private final String serviceName;

    private final String instanceId;

    private final String host;

    private final int port;

    private final int weight;

    private final Map<String, String> metadata;

    public ServiceInstance(String serviceName, String instanceId, String host, int port, int weight,
                           Map<String, String> metadata) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getWeight() {
        return weight;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}

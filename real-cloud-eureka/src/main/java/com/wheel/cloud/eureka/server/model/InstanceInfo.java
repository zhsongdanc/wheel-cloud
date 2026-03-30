package com.wheel.cloud.eureka.server.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InstanceInfo {

    private final String serviceName;

    private final String instanceId;

    private final String host;

    private final int port;

    private final String status;

    private final Map<String, String> metadata;

    public InstanceInfo(String serviceName, String instanceId, String host, int port, String status,
                        Map<String, String> metadata) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.port = port;
        this.status = status == null ? "UP" : status;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(metadata));
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

    public String getStatus() {
        return status;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}

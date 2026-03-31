package com.wheel.cloud.eureka.registry.model;

import java.util.List;
import java.util.Map;

public class RegistrySnapshot {

    private final int serviceCount;
    private final int instanceCount;
    private final Map<String, List<RegistryInstanceView>> services;

    public RegistrySnapshot(int serviceCount, int instanceCount, Map<String, List<RegistryInstanceView>> services) {
        this.serviceCount = serviceCount;
        this.instanceCount = instanceCount;
        this.services = services;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public Map<String, List<RegistryInstanceView>> getServices() {
        return services;
    }
}

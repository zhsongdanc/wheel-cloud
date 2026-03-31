package com.wheel.cloud.eureka.consumer.client.model;

import java.util.List;
import java.util.Map;

public class ClientRegistrySnapshot {

    private int serviceCount;
    private int instanceCount;
    private Map<String, List<ClientRegistryInstanceView>> services;

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public Map<String, List<ClientRegistryInstanceView>> getServices() {
        return services;
    }

    public void setServices(Map<String, List<ClientRegistryInstanceView>> services) {
        this.services = services;
    }
}

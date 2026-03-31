package com.wheel.cloud.eureka.registry.web;

public class RegistryNotFoundException extends RuntimeException {

    public RegistryNotFoundException(String serviceName, String instanceId) {
        super("instance not found: serviceName=" + serviceName + ", instanceId=" + instanceId);
    }
}

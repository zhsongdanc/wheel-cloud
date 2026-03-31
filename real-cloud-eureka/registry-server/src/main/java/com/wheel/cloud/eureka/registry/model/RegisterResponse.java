package com.wheel.cloud.eureka.registry.model;

public class RegisterResponse {

    private final String serviceName;
    private final String instanceId;
    private final long leaseDurationMs;
    private final String message;

    public RegisterResponse(String serviceName, String instanceId, long leaseDurationMs, String message) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.leaseDurationMs = leaseDurationMs;
        this.message = message;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public long getLeaseDurationMs() {
        return leaseDurationMs;
    }

    public String getMessage() {
        return message;
    }
}

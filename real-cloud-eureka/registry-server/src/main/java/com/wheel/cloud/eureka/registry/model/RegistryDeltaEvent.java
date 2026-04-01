package com.wheel.cloud.eureka.registry.model;

public class RegistryDeltaEvent {

    private final long version;
    private final DeltaEventType eventType;
    private final InstanceInfo instanceInfo;

    public RegistryDeltaEvent(long version, DeltaEventType eventType, InstanceInfo instanceInfo) {
        this.version = version;
        this.eventType = eventType;
        this.instanceInfo = instanceInfo;
    }

    public long getVersion() {
        return version;
    }

    public DeltaEventType getEventType() {
        return eventType;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }
}

package com.wheel.cloud.eureka.consumer.client.model;

public class RegistryDeltaEvent {

    private long version;
    private DeltaEventType eventType;
    private ClientInstanceInfo instanceInfo;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public DeltaEventType getEventType() {
        return eventType;
    }

    public void setEventType(DeltaEventType eventType) {
        this.eventType = eventType;
    }

    public ClientInstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public void setInstanceInfo(ClientInstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }
}

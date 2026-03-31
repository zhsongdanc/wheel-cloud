package com.wheel.cloud.eureka.registry.model;

public class RegistryInstanceView {

    private final InstanceInfo instanceInfo;
    private final LeaseView lease;

    public RegistryInstanceView(InstanceInfo instanceInfo, LeaseView lease) {
        this.instanceInfo = instanceInfo;
        this.lease = lease;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public LeaseView getLease() {
        return lease;
    }
}

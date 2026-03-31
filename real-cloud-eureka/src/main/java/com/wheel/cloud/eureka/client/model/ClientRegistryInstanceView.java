package com.wheel.cloud.eureka.client.model;

public class ClientRegistryInstanceView {

    private ClientInstanceInfo instanceInfo;

    private ClientLeaseView lease;

    public ClientInstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public void setInstanceInfo(ClientInstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }

    public ClientLeaseView getLease() {
        return lease;
    }

    public void setLease(ClientLeaseView lease) {
        this.lease = lease;
    }
}

package com.wheel.cloud.eureka.client.model;

public class ClientCacheView {

    private final boolean registered;

    private final long lastFetchTimestamp;

    private final ClientRegistrySnapshot snapshot;

    public ClientCacheView(boolean registered, long lastFetchTimestamp, ClientRegistrySnapshot snapshot) {
        this.registered = registered;
        this.lastFetchTimestamp = lastFetchTimestamp;
        this.snapshot = snapshot;
    }

    public boolean isRegistered() {
        return registered;
    }

    public long getLastFetchTimestamp() {
        return lastFetchTimestamp;
    }

    public ClientRegistrySnapshot getSnapshot() {
        return snapshot;
    }
}

package com.wheel.cloud.eureka.consumer.client.model;

public class ClientCacheView {

    private final boolean registered;
    private final long lastFetchTimestamp;
    private final long lastSeenVersion;
    private final ClientRegistrySnapshot snapshot;

    public ClientCacheView(boolean registered, long lastFetchTimestamp, long lastSeenVersion, ClientRegistrySnapshot snapshot) {
        this.registered = registered;
        this.lastFetchTimestamp = lastFetchTimestamp;
        this.lastSeenVersion = lastSeenVersion;
        this.snapshot = snapshot;
    }

    public boolean isRegistered() {
        return registered;
    }

    public long getLastFetchTimestamp() {
        return lastFetchTimestamp;
    }

    public long getLastSeenVersion() {
        return lastSeenVersion;
    }

    public ClientRegistrySnapshot getSnapshot() {
        return snapshot;
    }
}

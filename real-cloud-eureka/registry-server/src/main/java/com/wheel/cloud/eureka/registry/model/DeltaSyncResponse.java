package com.wheel.cloud.eureka.registry.model;

import java.util.List;

public class DeltaSyncResponse {

    private final long fromVersion;
    private final long toVersion;
    private final boolean fullSnapshotRequired;
    private final List<RegistryDeltaEvent> events;

    public DeltaSyncResponse(long fromVersion, long toVersion, boolean fullSnapshotRequired, List<RegistryDeltaEvent> events) {
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.fullSnapshotRequired = fullSnapshotRequired;
        this.events = events;
    }

    public long getFromVersion() {
        return fromVersion;
    }

    public long getToVersion() {
        return toVersion;
    }

    public boolean isFullSnapshotRequired() {
        return fullSnapshotRequired;
    }

    public List<RegistryDeltaEvent> getEvents() {
        return events;
    }
}

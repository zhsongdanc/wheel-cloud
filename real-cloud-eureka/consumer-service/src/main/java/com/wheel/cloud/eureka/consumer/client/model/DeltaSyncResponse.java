package com.wheel.cloud.eureka.consumer.client.model;

import java.util.List;

public class DeltaSyncResponse {

    private long fromVersion;
    private long toVersion;
    private boolean fullSnapshotRequired;
    private List<RegistryDeltaEvent> events;

    public long getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(long fromVersion) {
        this.fromVersion = fromVersion;
    }

    public long getToVersion() {
        return toVersion;
    }

    public void setToVersion(long toVersion) {
        this.toVersion = toVersion;
    }

    public boolean isFullSnapshotRequired() {
        return fullSnapshotRequired;
    }

    public void setFullSnapshotRequired(boolean fullSnapshotRequired) {
        this.fullSnapshotRequired = fullSnapshotRequired;
    }

    public List<RegistryDeltaEvent> getEvents() {
        return events;
    }

    public void setEvents(List<RegistryDeltaEvent> events) {
        this.events = events;
    }
}

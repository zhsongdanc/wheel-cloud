package com.wheel.cloud.loadbalancer.core.failover;

public class FailureRecord {

    private final long failUntilTimestamp;

    public FailureRecord(long failUntilTimestamp) {
        this.failUntilTimestamp = failUntilTimestamp;
    }

    public long getFailUntilTimestamp() {
        return failUntilTimestamp;
    }

    public boolean available(long now) {
        return now >= failUntilTimestamp;
    }
}

package com.wheel.cloud.eureka.server.model;

public class LeaseView {

    private final long registrationTimestamp;

    private final long lastRenewalTimestamp;

    private final long durationMs;

    private final boolean expired;

    public LeaseView(long registrationTimestamp, long lastRenewalTimestamp, long durationMs, boolean expired) {
        this.registrationTimestamp = registrationTimestamp;
        this.lastRenewalTimestamp = lastRenewalTimestamp;
        this.durationMs = durationMs;
        this.expired = expired;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public long getLastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isExpired() {
        return expired;
    }
}

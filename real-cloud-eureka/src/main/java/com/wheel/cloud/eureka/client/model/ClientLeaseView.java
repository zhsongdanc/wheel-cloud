package com.wheel.cloud.eureka.client.model;

public class ClientLeaseView {

    private long registrationTimestamp;

    private long lastRenewalTimestamp;

    private long durationMs;

    private boolean expired;

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public long getLastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public void setLastRenewalTimestamp(long lastRenewalTimestamp) {
        this.lastRenewalTimestamp = lastRenewalTimestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}

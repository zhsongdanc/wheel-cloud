package com.wheel.cloud.eureka.server.model;

public class Lease<T> {

    private final T holder;

    private final long durationMs;

    private volatile long registrationTimestamp;

    private volatile long lastRenewalTimestamp;

    public Lease(T holder, long durationMs, long now) {
        this.holder = holder;
        this.durationMs = durationMs;
        this.registrationTimestamp = now;
        this.lastRenewalTimestamp = now;
    }

    public T getHolder() {
        return holder;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public long getLastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public void renew(long now) {
        this.lastRenewalTimestamp = now;
    }

    public boolean isExpired(long now) {
        return now - lastRenewalTimestamp > durationMs;
    }
}

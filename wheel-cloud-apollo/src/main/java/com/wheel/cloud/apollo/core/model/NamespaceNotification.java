package com.wheel.cloud.apollo.core.model;

public class NamespaceNotification {

    private final String namespace;

    private final long notificationId;

    public NamespaceNotification(String namespace, long notificationId) {
        this.namespace = namespace;
        this.notificationId = notificationId;
    }

    public String getNamespace() {
        return namespace;
    }

    public long getNotificationId() {
        return notificationId;
    }
}

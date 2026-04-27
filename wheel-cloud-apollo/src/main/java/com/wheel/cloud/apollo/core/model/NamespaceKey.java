package com.wheel.cloud.apollo.core.model;

import java.util.Objects;

public class NamespaceKey {

    private final String appId;

    private final String cluster;

    private final String namespace;

    public NamespaceKey(String appId, String cluster, String namespace) {
        this.appId = Objects.requireNonNull(appId, "appId must not be null");
        this.cluster = Objects.requireNonNull(cluster, "cluster must not be null");
        this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
    }

    public String getAppId() {
        return appId;
    }

    public String getCluster() {
        return cluster;
    }

    public String getNamespace() {
        return namespace;
    }

    public String asString() {
        return appId + "+" + cluster + "+" + namespace;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NamespaceKey)) {
            return false;
        }
        NamespaceKey that = (NamespaceKey) object;
        return Objects.equals(appId, that.appId)
                && Objects.equals(cluster, that.cluster)
                && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, cluster, namespace);
    }
}

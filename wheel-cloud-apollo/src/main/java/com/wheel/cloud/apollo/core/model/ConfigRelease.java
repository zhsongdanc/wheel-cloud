package com.wheel.cloud.apollo.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigRelease {

    private final long releaseId;

    private final NamespaceKey namespaceKey;

    private final String releaseKey;

    private final Map<String, String> configurations;

    public ConfigRelease(long releaseId, NamespaceKey namespaceKey, String releaseKey, Map<String, String> configurations) {
        this.releaseId = releaseId;
        this.namespaceKey = namespaceKey;
        this.releaseKey = releaseKey;
        this.configurations = Collections.unmodifiableMap(new LinkedHashMap<>(configurations));
    }

    public long getReleaseId() {
        return releaseId;
    }

    public NamespaceKey getNamespaceKey() {
        return namespaceKey;
    }

    public String getReleaseKey() {
        return releaseKey;
    }

    public Map<String, String> getConfigurations() {
        return configurations;
    }
}

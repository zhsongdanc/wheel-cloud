package com.wheel.cloud.apollo.client.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientConfigCacheView {

    private final long lastMessageId;

    private final Map<String, String> configurations;

    public ClientConfigCacheView(long lastMessageId, Map<String, String> configurations) {
        this.lastMessageId = lastMessageId;
        this.configurations = Collections.unmodifiableMap(new LinkedHashMap<>(configurations));
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public Map<String, String> getConfigurations() {
        return configurations;
    }
}

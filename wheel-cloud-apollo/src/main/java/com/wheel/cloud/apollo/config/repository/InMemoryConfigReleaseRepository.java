package com.wheel.cloud.apollo.config.repository;

import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceKey;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryConfigReleaseRepository implements ConfigReleaseRepository {

    private final ConcurrentMap<NamespaceKey, ConfigRelease> latestReleaseMap = new ConcurrentHashMap<>();

    @Override
    public void save(ConfigRelease release) {
        latestReleaseMap.put(release.getNamespaceKey(), release);
    }

    @Override
    public ConfigRelease findLatestRelease(NamespaceKey namespaceKey) {
        return latestReleaseMap.get(namespaceKey);
    }
}

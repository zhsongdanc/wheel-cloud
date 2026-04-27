package com.wheel.cloud.apollo.config.repository;

import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceKey;

public interface ConfigReleaseRepository {

    void save(ConfigRelease release);

    ConfigRelease findLatestRelease(NamespaceKey namespaceKey);
}

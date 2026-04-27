package com.wheel.cloud.apollo.config.service;

import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceNotification;

import java.util.List;

public interface ConfigReadService {

    ConfigRelease getLatestRelease(String appId, String cluster, String namespace);

    List<NamespaceNotification> findUpdatedNamespaces(String appId, String cluster, List<String> namespaces, long messageId);
}

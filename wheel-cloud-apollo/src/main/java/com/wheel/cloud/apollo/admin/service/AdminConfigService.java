package com.wheel.cloud.apollo.admin.service;

import com.wheel.cloud.apollo.admin.model.PublishNamespaceRequest;
import com.wheel.cloud.apollo.core.model.ConfigRelease;

public interface AdminConfigService {

    ConfigRelease publish(PublishNamespaceRequest request);
}

package com.wheel.cloud.apollo.admin.service;

import com.wheel.cloud.apollo.admin.model.PublishNamespaceRequest;
import com.wheel.cloud.apollo.config.repository.ConfigReleaseRepository;
import com.wheel.cloud.apollo.config.repository.ReleaseMessageRepository;
import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceKey;
import com.wheel.cloud.apollo.core.model.ReleaseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DefaultAdminConfigService implements AdminConfigService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminConfigService.class);

    private final AtomicLong releaseIdGenerator = new AtomicLong(1000L);

    private final ConfigReleaseRepository configReleaseRepository;

    private final ReleaseMessageRepository releaseMessageRepository;

    public DefaultAdminConfigService(ConfigReleaseRepository configReleaseRepository,
                                     ReleaseMessageRepository releaseMessageRepository) {
        this.configReleaseRepository = configReleaseRepository;
        this.releaseMessageRepository = releaseMessageRepository;
    }

    @Override
    public ConfigRelease publish(PublishNamespaceRequest request) {
        validateRequest(request);
        NamespaceKey namespaceKey = new NamespaceKey(request.getAppId(), request.getCluster(), request.getNamespace());
        long releaseId = releaseIdGenerator.incrementAndGet();
        String releaseKey = namespaceKey.asString() + ":" + releaseId;

        // 模仿 Apollo 的关键设计：发布配置和通知客户端更新之间通过 ReleaseMessage 解耦。
        ConfigRelease release = new ConfigRelease(releaseId, namespaceKey, releaseKey, request.getConfigurations());
        configReleaseRepository.save(release);
        ReleaseMessage releaseMessage = releaseMessageRepository.append(namespaceKey, releaseKey);

        log.info("config published: appId={}, cluster={}, namespace={}, releaseId={}, messageId={}",
                request.getAppId(), request.getCluster(), request.getNamespace(), releaseId, releaseMessage.getMessageId());
        return release;
    }

    private void validateRequest(PublishNamespaceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (!StringUtils.hasText(request.getAppId())) {
            throw new IllegalArgumentException("appId must not be blank");
        }
        if (!StringUtils.hasText(request.getCluster())) {
            throw new IllegalArgumentException("cluster must not be blank");
        }
        if (!StringUtils.hasText(request.getNamespace())) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        Map<String, String> configurations = request.getConfigurations();
        if (configurations == null || configurations.isEmpty()) {
            throw new IllegalArgumentException("configurations must not be empty");
        }
    }
}

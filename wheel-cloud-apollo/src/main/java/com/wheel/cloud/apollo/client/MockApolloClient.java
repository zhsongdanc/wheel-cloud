package com.wheel.cloud.apollo.client;

import com.wheel.cloud.apollo.client.model.ClientConfigCacheView;
import com.wheel.cloud.apollo.config.service.ConfigReadService;
import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MockApolloClient {

    private static final Logger log = LoggerFactory.getLogger(MockApolloClient.class);

    private final ConfigReadService configReadService;

    private final AtomicLong lastMessageId = new AtomicLong(0L);

    private final AtomicReference<Map<String, String>> localCache = new AtomicReference<>(Collections.emptyMap());

    public MockApolloClient(ConfigReadService configReadService) {
        this.configReadService = configReadService;
    }

    public ClientConfigCacheView refresh(String appId, String cluster, String namespace) {
        List<NamespaceNotification> notifications = configReadService.findUpdatedNamespaces(
                appId,
                cluster,
                Collections.singletonList(namespace),
                lastMessageId.get()
        );
        if (notifications.isEmpty()) {
            return new ClientConfigCacheView(lastMessageId.get(), localCache.get());
        }

        ConfigRelease latestRelease = configReadService.getLatestRelease(appId, cluster, namespace);
        if (latestRelease != null) {
            // 模仿 Apollo client 的核心味道：业务读取始终走本地缓存，不把配置中心放进主链路。
            localCache.set(new LinkedHashMap<>(latestRelease.getConfigurations()));
            long maxMessageId = notifications.stream()
                    .mapToLong(NamespaceNotification::getNotificationId)
                    .max()
                    .orElse(lastMessageId.get());
            lastMessageId.set(maxMessageId);
            log.info("apollo client refreshed local cache: appId={}, cluster={}, namespace={}, lastMessageId={}",
                    appId, cluster, namespace, maxMessageId);
        }
        return new ClientConfigCacheView(lastMessageId.get(), localCache.get());
    }

    public ClientConfigCacheView getCacheView() {
        return new ClientConfigCacheView(lastMessageId.get(), localCache.get());
    }
}

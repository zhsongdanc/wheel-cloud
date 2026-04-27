package com.wheel.cloud.apollo.config.service;

import com.wheel.cloud.apollo.config.repository.ConfigReleaseRepository;
import com.wheel.cloud.apollo.config.repository.ReleaseMessageRepository;
import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceKey;
import com.wheel.cloud.apollo.core.model.NamespaceNotification;
import com.wheel.cloud.apollo.core.model.ReleaseMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultConfigReadService implements ConfigReadService {

    private final ConfigReleaseRepository configReleaseRepository;

    private final ReleaseMessageRepository releaseMessageRepository;

    public DefaultConfigReadService(ConfigReleaseRepository configReleaseRepository,
                                    ReleaseMessageRepository releaseMessageRepository) {
        this.configReleaseRepository = configReleaseRepository;
        this.releaseMessageRepository = releaseMessageRepository;
    }

    @Override
    public ConfigRelease getLatestRelease(String appId, String cluster, String namespace) {
        return configReleaseRepository.findLatestRelease(new NamespaceKey(appId, cluster, namespace));
    }

    @Override
    public List<NamespaceNotification> findUpdatedNamespaces(String appId, String cluster, List<String> namespaces, long messageId) {
        Set<String> namespaceSet = new LinkedHashSet<>(namespaces);
        List<ReleaseMessage> messages = releaseMessageRepository.findMessagesAfter(messageId).stream()
                .filter(message -> message.getNamespaceKey().getAppId().equals(appId))
                .filter(message -> message.getNamespaceKey().getCluster().equals(cluster))
                .filter(message -> namespaceSet.contains(message.getNamespaceKey().getNamespace()))
                .collect(Collectors.toList());

        List<NamespaceNotification> notifications = new ArrayList<>();
        for (ReleaseMessage message : messages) {
            notifications.add(new NamespaceNotification(
                    message.getNamespaceKey().getNamespace(),
                    message.getMessageId()
            ));
        }
        return notifications;
    }
}

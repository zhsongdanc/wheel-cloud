package com.wheel.cloud.apollo.config.repository;

import com.wheel.cloud.apollo.core.model.NamespaceKey;
import com.wheel.cloud.apollo.core.model.ReleaseMessage;

import java.util.List;

public interface ReleaseMessageRepository {

    ReleaseMessage append(NamespaceKey namespaceKey, String releaseKey);

    List<ReleaseMessage> findMessagesAfter(long messageId);

    long latestMessageId();
}

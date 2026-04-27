package com.wheel.cloud.apollo.config.repository;

import com.wheel.cloud.apollo.core.model.NamespaceKey;
import com.wheel.cloud.apollo.core.model.ReleaseMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryReleaseMessageRepository implements ReleaseMessageRepository {

    private final AtomicLong messageIdGenerator = new AtomicLong(0L);

    private final CopyOnWriteArrayList<ReleaseMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public ReleaseMessage append(NamespaceKey namespaceKey, String releaseKey) {
        long messageId = messageIdGenerator.incrementAndGet();
        ReleaseMessage message = new ReleaseMessage(messageId, namespaceKey, releaseKey);
        messages.add(message);
        return message;
    }

    @Override
    public List<ReleaseMessage> findMessagesAfter(long messageId) {
        return messages.stream()
                .filter(message -> message.getMessageId() > messageId)
                .collect(Collectors.toList());
    }

    @Override
    public long latestMessageId() {
        return messageIdGenerator.get();
    }
}

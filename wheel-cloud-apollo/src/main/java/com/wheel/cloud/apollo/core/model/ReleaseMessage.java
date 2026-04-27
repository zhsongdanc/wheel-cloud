package com.wheel.cloud.apollo.core.model;

public class ReleaseMessage {

    private final long messageId;

    private final NamespaceKey namespaceKey;

    private final String releaseKey;

    public ReleaseMessage(long messageId, NamespaceKey namespaceKey, String releaseKey) {
        this.messageId = messageId;
        this.namespaceKey = namespaceKey;
        this.releaseKey = releaseKey;
    }

    public long getMessageId() {
        return messageId;
    }

    public NamespaceKey getNamespaceKey() {
        return namespaceKey;
    }

    public String getReleaseKey() {
        return releaseKey;
    }
}

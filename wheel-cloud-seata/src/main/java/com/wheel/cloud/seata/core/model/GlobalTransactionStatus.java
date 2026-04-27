package com.wheel.cloud.seata.core.model;

public enum GlobalTransactionStatus {
    BEGIN,
    COMMITTING,
    COMMITTED,
    ROLLBACKING,
    ROLLBACKED,
    TIMEOUT_ROLLBACKING,
    UNKNOWN
}

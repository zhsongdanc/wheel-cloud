package com.wheel.cloud.seata.core.model;

public enum BranchStatus {
    REGISTERED,
    PHASE_ONE_DONE,
    PHASE_ONE_FAILED,
    PHASE_TWO_COMMITTING,
    PHASE_TWO_COMMITTED,
    PHASE_TWO_ROLLBACKING,
    PHASE_TWO_ROLLBACKED,
    UNKNOWN
}

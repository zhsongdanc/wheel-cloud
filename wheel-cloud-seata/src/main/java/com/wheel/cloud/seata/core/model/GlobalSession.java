package com.wheel.cloud.seata.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlobalSession {

    private final long xid;

    private final String applicationId;

    private final String transactionName;

    private final List<BranchSession> branches = new ArrayList<>();

    private volatile GlobalTransactionStatus status;

    public GlobalSession(long xid, String applicationId, String transactionName, GlobalTransactionStatus status) {
        this.xid = xid;
        this.applicationId = applicationId;
        this.transactionName = transactionName;
        this.status = status;
    }

    public long getXid() {
        return xid;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public synchronized void addBranch(BranchSession branchSession) {
        branches.add(branchSession);
    }

    public synchronized List<BranchSession> getBranches() {
        return Collections.unmodifiableList(new ArrayList<>(branches));
    }

    public GlobalTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(GlobalTransactionStatus status) {
        this.status = status;
    }
}

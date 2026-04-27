package com.wheel.cloud.seata.core.model;

public class BranchSession {

    private final long branchId;

    private final long xid;

    private final String resourceId;

    private final String branchType;

    private volatile BranchStatus status;

    public BranchSession(long branchId, long xid, String resourceId, String branchType, BranchStatus status) {
        this.branchId = branchId;
        this.xid = xid;
        this.resourceId = resourceId;
        this.branchType = branchType;
        this.status = status;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getXid() {
        return xid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getBranchType() {
        return branchType;
    }

    public BranchStatus getStatus() {
        return status;
    }

    public void setStatus(BranchStatus status) {
        this.status = status;
    }
}

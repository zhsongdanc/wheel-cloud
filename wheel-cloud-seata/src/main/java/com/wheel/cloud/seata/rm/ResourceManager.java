package com.wheel.cloud.seata.rm;

import com.wheel.cloud.seata.core.model.BranchSession;

public interface ResourceManager {

    BranchSession registerBranch(long xid, String resourceId, String branchType);
}

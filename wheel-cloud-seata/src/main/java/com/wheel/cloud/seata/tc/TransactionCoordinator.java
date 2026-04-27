package com.wheel.cloud.seata.tc;

import com.wheel.cloud.seata.core.model.BranchSession;
import com.wheel.cloud.seata.core.model.GlobalSession;

public interface TransactionCoordinator {

    GlobalSession begin(String applicationId, String transactionName);

    BranchSession registerBranch(long xid, String resourceId, String branchType);

    GlobalSession commit(long xid);

    GlobalSession rollback(long xid);

    GlobalSession findGlobalSession(long xid);
}

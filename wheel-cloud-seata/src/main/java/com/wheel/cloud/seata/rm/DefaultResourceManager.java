package com.wheel.cloud.seata.rm;

import com.wheel.cloud.seata.core.model.BranchSession;
import com.wheel.cloud.seata.tc.TransactionCoordinator;
import org.springframework.stereotype.Component;

@Component
public class DefaultResourceManager implements ResourceManager {

    private final TransactionCoordinator transactionCoordinator;

    public DefaultResourceManager(TransactionCoordinator transactionCoordinator) {
        this.transactionCoordinator = transactionCoordinator;
    }

    @Override
    public BranchSession registerBranch(long xid, String resourceId, String branchType) {
        return transactionCoordinator.registerBranch(xid, resourceId, branchType);
    }
}

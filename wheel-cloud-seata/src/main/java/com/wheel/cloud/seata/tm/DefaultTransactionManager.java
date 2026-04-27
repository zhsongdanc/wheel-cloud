package com.wheel.cloud.seata.tm;

import com.wheel.cloud.seata.core.model.GlobalSession;
import com.wheel.cloud.seata.tc.TransactionCoordinator;
import org.springframework.stereotype.Component;

@Component
public class DefaultTransactionManager implements TransactionManager {

    private final TransactionCoordinator transactionCoordinator;

    public DefaultTransactionManager(TransactionCoordinator transactionCoordinator) {
        this.transactionCoordinator = transactionCoordinator;
    }

    @Override
    public GlobalSession begin(String applicationId, String transactionName) {
        return transactionCoordinator.begin(applicationId, transactionName);
    }

    @Override
    public GlobalSession commit(long xid) {
        return transactionCoordinator.commit(xid);
    }

    @Override
    public GlobalSession rollback(long xid) {
        return transactionCoordinator.rollback(xid);
    }
}

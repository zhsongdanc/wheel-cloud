package com.wheel.cloud.seata.tc;

import com.wheel.cloud.seata.core.model.BranchSession;
import com.wheel.cloud.seata.core.model.BranchStatus;
import com.wheel.cloud.seata.core.model.GlobalSession;
import com.wheel.cloud.seata.core.model.GlobalTransactionStatus;
import com.wheel.cloud.seata.tc.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DefaultTransactionCoordinator implements TransactionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(DefaultTransactionCoordinator.class);

    private final AtomicLong xidGenerator = new AtomicLong(1000L);

    private final AtomicLong branchIdGenerator = new AtomicLong(1L);

    private final SessionStore sessionStore;

    public DefaultTransactionCoordinator(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public GlobalSession begin(String applicationId, String transactionName) {
        long xid = xidGenerator.incrementAndGet();
        GlobalSession globalSession = new GlobalSession(xid, applicationId, transactionName, GlobalTransactionStatus.BEGIN);
        sessionStore.store(globalSession);
        log.info("global transaction begin: xid={}, applicationId={}, transactionName={}", xid, applicationId, transactionName);
        return globalSession;
    }

    @Override
    public BranchSession registerBranch(long xid, String resourceId, String branchType) {
        GlobalSession globalSession = requireGlobalSession(xid);
        long branchId = branchIdGenerator.incrementAndGet();
        BranchSession branchSession = new BranchSession(branchId, xid, resourceId, branchType, BranchStatus.REGISTERED);
        globalSession.addBranch(branchSession);
        log.info("branch registered: xid={}, branchId={}, resourceId={}, branchType={}",
                xid, branchId, resourceId, branchType);
        return branchSession;
    }

    @Override
    public GlobalSession commit(long xid) {
        GlobalSession globalSession = requireGlobalSession(xid);
        globalSession.setStatus(GlobalTransactionStatus.COMMITTING);
        globalSession.setStatus(GlobalTransactionStatus.COMMITTED);
        log.info("global transaction committed: xid={}", xid);
        return globalSession;
    }

    @Override
    public GlobalSession rollback(long xid) {
        GlobalSession globalSession = requireGlobalSession(xid);
        globalSession.setStatus(GlobalTransactionStatus.ROLLBACKING);
        globalSession.setStatus(GlobalTransactionStatus.ROLLBACKED);
        log.info("global transaction rolled back: xid={}", xid);
        return globalSession;
    }

    @Override
    public GlobalSession findGlobalSession(long xid) {
        return sessionStore.findGlobalSession(xid);
    }

    private GlobalSession requireGlobalSession(long xid) {
        GlobalSession globalSession = sessionStore.findGlobalSession(xid);
        if (globalSession == null) {
            throw new IllegalArgumentException("global session not found, xid=" + xid);
        }
        return globalSession;
    }
}

package com.wheel.cloud.seata.tm;

import com.wheel.cloud.seata.core.model.GlobalSession;

public interface TransactionManager {

    GlobalSession begin(String applicationId, String transactionName);

    GlobalSession commit(long xid);

    GlobalSession rollback(long xid);
}

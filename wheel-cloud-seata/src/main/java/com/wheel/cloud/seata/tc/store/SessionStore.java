package com.wheel.cloud.seata.tc.store;

import com.wheel.cloud.seata.core.model.GlobalSession;

public interface SessionStore {

    void store(GlobalSession globalSession);

    GlobalSession findGlobalSession(long xid);
}

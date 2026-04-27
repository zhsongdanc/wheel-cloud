package com.wheel.cloud.seata.tc.store;

import com.wheel.cloud.seata.core.model.GlobalSession;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentMap<Long, GlobalSession> globalSessionMap = new ConcurrentHashMap<>();

    @Override
    public void store(GlobalSession globalSession) {
        globalSessionMap.put(globalSession.getXid(), globalSession);
    }

    @Override
    public GlobalSession findGlobalSession(long xid) {
        return globalSessionMap.get(xid);
    }
}

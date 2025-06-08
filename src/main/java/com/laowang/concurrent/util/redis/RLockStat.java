package com.laowang.concurrent.util.redis;

import org.redisson.api.RLock;

public class RLockStat implements AutoCloseable {
    private final RLock rLock;
    private final boolean isLocked;

    public RLockStat(RLock rLock, boolean isLocked) {
        this.rLock = rLock;
        this.isLocked = isLocked;
    }

    public RLock getLock() {
        return rLock;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void close() throws Exception {
        if (isLocked) {
            rLock.unlock();
        }
    }
}
package com.laowang.concurrent.util;

import org.redisson.api.RLock;

import java.util.concurrent.locks.Lock;

public class RLockStat implements AutoCloseable {
    private final RLock rLock;
    private final boolean isLocked;

    public RLockStat(RLock rLock, boolean isLocked) {
        this.rLock = rLock;
        this.isLocked = isLocked;
    }

    public Lock getLock() {
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

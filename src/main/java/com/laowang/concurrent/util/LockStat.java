package com.laowang.concurrent.util;

import java.util.concurrent.locks.Lock;

/**
 * Lock status wrapper class that implements AutoCloseable for try-with-resources support
 */
public class LockStat implements AutoCloseable {
    private final Lock lock;
    private final boolean isLocked;

    public LockStat(Lock lock, boolean isLocked) {
        this.lock = lock;
        this.isLocked = isLocked;
    }

    public Lock getLock() {
        return lock;
    }

    public boolean isLocked() {
        return isLocked;
    }


    @Override
    public void close() throws Exception {
        if (isLocked) {
            lock.unlock();
        }
    }
}

package com.laowang.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class LockUtils {

    public static LockStat lock(Lock lock) {
        lock.lock();
        return new LockStat(lock, true);
    }

    public static LockStat tryLock(Lock lock) {
        boolean tryLock = lock.tryLock();
        return new LockStat(lock, tryLock);
    }

    public static LockStat tryLock(Lock lock, long timeout, TimeUnit timeUnit) {
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(timeout, timeUnit);
        } catch (InterruptedException e) {
            // Log warning to console since no logging framework is available
            System.err.println("Warning: lock failed due to thread interruption");
        }
        return new LockStat(lock, tryLock);
    }

    /**
     * Lock status wrapper class that implements AutoCloseable for try-with-resources support
     */
    public static class LockStat implements AutoCloseable {
        private Lock lock;
        private boolean isLocked;

        public LockStat(Lock lock, boolean isLocked) {
            this.lock = lock;
            this.isLocked = isLocked;
        }

        public Lock getLock() {
            return lock;
        }

        public void setLock(Lock lock) {
            this.lock = lock;
        }

        public boolean isLocked() {
            return isLocked;
        }

        public void setLocked(boolean locked) {
            isLocked = locked;
        }

        @Override
        public void close() throws Exception {
            if (isLocked) {
                lock.unlock();
            }
        }
    }
}

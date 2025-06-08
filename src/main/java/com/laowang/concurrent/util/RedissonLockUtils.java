package com.laowang.concurrent.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class RedissonLockUtils {

    private static RedissonClient redissonClient;

    public static void init(RedissonClient redissonClient) {
        // auto inject  redissonClient
        RedissonLockUtils.redissonClient = redissonClient;
    }

    private static RLock getLock(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock;
    }

    public static RLockStat tryLock(RLock lock, long waitTime, long leaseTime, TimeUnit timeUnit) {
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            System.err.println("Warning: lock failed due to thread interruption");
        }
        return new RLockStat(lock, tryLock);
    }

    public static RLockStat lock(RLock lock, long leaseTime, TimeUnit timeUnit) {
        lock.lock(leaseTime, timeUnit);
        return new RLockStat(lock, true);
    }

    public static RLockStat tryLock(RLock lock, long timeout, TimeUnit timeUnit) {
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(timeout, timeUnit);
        } catch (InterruptedException e) {
            System.err.println("Warning: lock failed due to thread interruption");
        }
        return new RLockStat(lock, tryLock);
    }

    public static RLockStat tryLock(RLock lock) {
        boolean tryLock = lock.tryLock();
        return new RLockStat(lock, tryLock);
    }

    public static RLockStat lock(RLock lock) {
        lock.lock();
        return new RLockStat(lock, true);
    }

    public static RLockStat tryLock(String lockName, long timeout, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return tryLock(lock, timeout, timeUnit);
    }

    public static RLockStat tryLock(String lockName) {
        RLock lock = getLock(lockName);
        return tryLock(lock);
    }

    public static RLockStat lock(String lockName) {
        RLock lock = getLock(lockName);
        return lock(lock);
    }

    public static RLockStat tryLock(String lockName, long waitTime, long timeout, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return tryLock(lock, waitTime, timeout, timeUnit);
    }

    public static RLockStat lock(String lockName, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return lock(lock, leaseTime, timeUnit);
    }

    public static class RLockStat implements AutoCloseable {
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
}

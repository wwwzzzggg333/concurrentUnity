package com.laowang.concurrent.util.redis;

import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

public class RedissonLockUtils {

    private static RLock getLock(String key) {
        return RedissonUtils.getLock(key);
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

    public static RLockStat tryLock(RLock lock, long leaseTime, TimeUnit timeUnit) {
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(leaseTime, timeUnit);
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

    public static RLockStat tryLock(String lockName, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return tryLock(lock, leaseTime, timeUnit);
    }

    public static RLockStat tryLock(String lockName) {
        RLock lock = getLock(lockName);
        return tryLock(lock);
    }

    public static RLockStat lock(String lockName) {
        RLock lock = getLock(lockName);
        return lock(lock);
    }

    public static RLockStat tryLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return tryLock(lock, waitTime, leaseTime, timeUnit);
    }

    public static RLockStat lock(String lockName, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockName);
        return lock(lock, leaseTime, timeUnit);
    }

}

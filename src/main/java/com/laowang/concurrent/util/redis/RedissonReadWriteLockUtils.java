package com.laowang.concurrent.util.redis;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;

import java.util.concurrent.TimeUnit;

public class RedissonReadWriteLockUtils {

    private static RReadWriteLock getLock(String key) {
        return RedissonUtils.getRWLock(key);
    }

    private static RLock getReadLock(String key) {
        return getLock(key).readLock();
    }

    private static RLock getWriteLock(String key) {
        return getLock(key).writeLock();
    }

    public static RLockStat tryReadLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getReadLock(lockName);
        return RedissonLockUtils.tryLock(lock, waitTime, leaseTime, timeUnit);
    }

    public static RLockStat readLock(String lockName, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getReadLock(lockName);
        return RedissonLockUtils.lock(lock, leaseTime, timeUnit);
    }

    public static RLockStat tryReadLock(String lockName, long timeout, TimeUnit timeUnit) {
        RLock lock = getReadLock(lockName);
        return RedissonLockUtils.tryLock(lock, timeout, timeUnit);
    }

    public static RLockStat tryReadLock(String lockName) {
        RLock lock = getReadLock(lockName);
        return RedissonLockUtils.tryLock(lock);
    }

    public static RLockStat readlock(String lockName) {
        RLock lock = getReadLock(lockName);
        return RedissonLockUtils.lock(lock);
    }

    public static RLockStat tryWriteLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getWriteLock(lockName);
        return RedissonLockUtils.tryLock(lock, waitTime, leaseTime, timeUnit);
    }

    public static RLockStat writeLock(String lockName, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getWriteLock(lockName);
        return RedissonLockUtils.lock(lock, leaseTime, timeUnit);
    }

    public static RLockStat tryWriteLock(String lockName, long timeout, TimeUnit timeUnit) {
        RLock lock = getWriteLock(lockName);
        return RedissonLockUtils.tryLock(lock, timeout, timeUnit);
    }

    public static RLockStat tryWriteLock(String lockName) {
        RLock lock = getWriteLock(lockName);
        return RedissonLockUtils.tryLock(lock);
    }

    public static RLockStat writelock(String lockName) {
        RLock lock = getWriteLock(lockName);
        return RedissonLockUtils.lock(lock);
    }

}

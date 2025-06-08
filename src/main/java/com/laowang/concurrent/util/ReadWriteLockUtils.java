package com.laowang.concurrent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockUtils {

    private static final Map<String, ReadWriteLock> LOCKS = new ConcurrentHashMap<>();

    private static ReadWriteLock getLock(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }

    private static Lock getReadLock(String key) {
        return getLock(key).readLock();
    }

    private static Lock getWriteLock(String key) {
        return getLock(key).writeLock();
    }

    public static LockStat tryReadLock(String lockName, long timeout, TimeUnit timeUnit) {
        Lock lock = getReadLock(lockName);
        return LockUtils.tryLock(lock, timeout, timeUnit);
    }

    public static LockStat tryReadLock(String lockName) {
        Lock lock = getReadLock(lockName);
        return LockUtils.tryLock(lock);
    }

    public static LockStat readlock(String lockName) {
        Lock lock = getReadLock(lockName);
        return LockUtils.lock(lock);
    }

    public static LockStat tryWriteLock(String lockName, long timeout, TimeUnit timeUnit) {
        Lock lock = getWriteLock(lockName);
        return LockUtils.tryLock(lock, timeout, timeUnit);
    }

    public static LockStat tryWriteLock(String lockName) {
        Lock lock = getWriteLock(lockName);
        return LockUtils.tryLock(lock);
    }

    public static LockStat writelock(String lockName) {
        Lock lock = getWriteLock(lockName);
        return LockUtils.lock(lock);
    }

}

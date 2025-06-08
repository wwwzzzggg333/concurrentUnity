package com.laowang.concurrent.util;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockUtils {

    private static final Map<String, LockInfo> LOCKS = new ConcurrentHashMap<>();

    private static LockInfo getLock(String key) {
        LockInfo lockInfo = LOCKS.get(key);
        if (lockInfo != null) {
            return lockInfo;
        }
        synchronized (LOCKS) {
            lockInfo = LOCKS.get(key);
            if (lockInfo != null) {
                return lockInfo;
            }

            lockInfo = new LockInfo();
            LOCKS.put(key, lockInfo);

            ReadWriteLock rwLock = new ReentrantReadWriteLock();
            Lock readLock = rwLock.readLock();
            Lock writeLock = rwLock.writeLock();
            lockInfo.setReadWriteLock(rwLock);
            lockInfo.setReadLock(readLock);
            lockInfo.setWriteLock(writeLock);
            return lockInfo;
        }
    }

    private static Lock getReadLock(String key) {
        return getLock(key).getReadLock();
    }

    private static Lock getWriteLock(String key) {
        return getLock(key).getWriteLock();
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

    @Data
    private static class LockInfo {
        private ReadWriteLock readWriteLock;
        private Lock readLock;
        private Lock writeLock;
    }
}

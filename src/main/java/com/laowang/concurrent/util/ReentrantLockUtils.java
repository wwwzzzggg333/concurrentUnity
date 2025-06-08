package com.laowang.concurrent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockUtils {

    private static final Map<String, Lock> LOCKS = new ConcurrentHashMap<>();

    private static Lock getLock(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public static LockStat tryLock(String lockName, long timeout, TimeUnit timeUnit) {
        Lock lock = getLock(lockName);
        return LockUtils.tryLock(lock, timeout, timeUnit);
    }

    public static LockStat tryLock(String lockName) {
        Lock lock = getLock(lockName);
        return LockUtils.tryLock(lock);
    }

    public static LockStat lock(String lockName) {
        Lock lock = getLock(lockName);
        return LockUtils.lock(lock);
    }
}

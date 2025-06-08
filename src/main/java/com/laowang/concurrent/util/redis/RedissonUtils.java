package com.laowang.concurrent.util.redis;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

public class RedissonUtils {

    private static RedissonClient redissonClient;

    public static void setUp(RedissonClient redissonClient) {
        RedissonUtils.redissonClient = redissonClient;
    }

    public static RLock getLock(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock;
    }

    public static RReadWriteLock getRWLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }
}

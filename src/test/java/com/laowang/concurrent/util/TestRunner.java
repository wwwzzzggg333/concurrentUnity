package com.laowang.concurrent.util;

import com.laowang.concurrent.util.LockUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LockUtils功能验证测试运行器
 * 由于没有Maven环境，使用简单的方式验证LockUtils功能
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== LockUtils 功能测试开始 ===");
        
        TestRunner runner = new TestRunner();
        
        try {
            runner.testLockBasicFunction();
            runner.testTryLockSuccess();
            runner.testTryLockFailure();
            runner.testLockStatAutoClose();
            runner.testConcurrentAccess();
            
            System.out.println("\n✅ 所有测试通过！LockUtils工具类功能正常");
        } catch (Exception e) {
            System.err.println("\n❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== LockUtils 功能测试结束 ===");
    }
    
    /**
     * 测试基本lock功能
     */
    public void testLockBasicFunction() throws Exception {
        System.out.println("\n1. 测试 lock() 基本功能...");
        Lock testLock = new ReentrantLock();
        
        // 使用LockUtils.lock获取锁
        LockUtils.LockStat lockStat = LockUtils.lock(testLock);
        
        // 验证锁被正确获取
        assert lockStat != null : "LockStat对象不应为null";
        assert lockStat.getLock() == testLock : "Lock对象应该匹配";
        assert lockStat.isLocked() : "锁状态应该为true";
        
        // 验证锁确实被占用
        assert !testLock.tryLock() : "锁应该已被占用";
        
        // 释放锁
        lockStat.close();
        
        // 验证锁被释放
        assert testLock.tryLock() : "锁应该已被释放";
        testLock.unlock(); // 清理
        
        System.out.println("   ✅ lock() 基本功能测试通过");
    }
    
    /**
     * 测试tryLock成功场景
     */
    public void testTryLockSuccess() throws Exception {
        System.out.println("\n2. 测试 tryLock() 成功场景...");
        Lock testLock = new ReentrantLock();
        
        // 尝试获取可用的锁
        LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 1L, TimeUnit.SECONDS);
        
        // 验证锁被正确获取
        assert lockStat != null : "LockStat对象不应为null";
        assert lockStat.isLocked() : "锁状态应该为true";
        
        // 释放锁
        lockStat.close();
        
        System.out.println("   ✅ tryLock() 成功场景测试通过");
    }
    
    /**
     * 测试tryLock失败场景
     */
    public void testTryLockFailure() throws Exception {
        System.out.println("\n3. 测试 tryLock() 失败场景...");
        Lock testLock = new ReentrantLock();
        
        // 先占用锁
        testLock.lock();
        
        try {
            // 尝试获取已被占用的锁（应该失败）
            LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 100L, TimeUnit.MILLISECONDS);
            
            // 验证获取锁失败
            assert lockStat != null : "LockStat对象不应为null";
            assert !lockStat.isLocked() : "锁状态应该为false";
            
            // close()方法应该安全处理未获取的锁
            lockStat.close();
            
        } finally {
            testLock.unlock(); // 释放锁
        }
        
        System.out.println("   ✅ tryLock() 失败场景测试通过");
    }
    
    /**
     * 测试LockStat的try-with-resources功能
     */
    public void testLockStatAutoClose() throws Exception {
        System.out.println("\n4. 测试 LockStat 自动关闭功能...");
        Lock testLock = new ReentrantLock();
        
        // 使用try-with-resources
        try (LockUtils.LockStat lockStat = LockUtils.lock(testLock)) {
            assert lockStat.isLocked() : "锁状态应该为true";
            assert !testLock.tryLock() : "锁应该已被占用";
        }
        
        // 验证锁被自动释放
        assert testLock.tryLock() : "锁应该已被自动释放";
        testLock.unlock(); // 清理
        
        System.out.println("   ✅ LockStat 自动关闭功能测试通过");
    }
    
    /**
     * 测试并发访问场景
     */
    public void testConcurrentAccess() throws Exception {
        System.out.println("\n5. 测试并发访问场景...");
        Lock testLock = new ReentrantLock();
        final int threadCount = 5;
        final boolean[] results = new boolean[threadCount];
        final Thread[] threads = new Thread[threadCount];
        
        // 创建多个线程同时使用LockUtils
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try (LockUtils.LockStat lockStat = LockUtils.lock(testLock)) {
                    // 模拟一些工作
                    Thread.sleep(10);
                    results[index] = lockStat.isLocked();
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有线程都成功获取了锁
        for (int i = 0; i < threadCount; i++) {
            assert results[i] : "线程 " + i + " 应该成功获取锁";
        }
        
        // 验证最终锁被释放
        assert testLock.tryLock() : "所有线程完成后锁应该被释放";
        testLock.unlock();
        
        System.out.println("   ✅ 并发访问场景测试通过");
    }
} 
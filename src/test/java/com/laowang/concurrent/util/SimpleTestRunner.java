package com.laowang.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.laowang.concurrent.util.LockUtils;

/**
 * Simple test runner for LockUtils functionality verification
 */
public class SimpleTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== LockUtils Test Started ===");
        
        SimpleTestRunner runner = new SimpleTestRunner();
        
        try {
            runner.testBasicLock();
            runner.testTryLockSuccess();
            runner.testTryLockFailure();
            runner.testAutoClose();
            
            System.out.println("\nAll tests passed! LockUtils works correctly.");
        } catch (Exception e) {
            System.err.println("\nTest failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== LockUtils Test Finished ===");
    }
    
    public void testBasicLock() throws Exception {
        System.out.println("\n1. Testing basic lock functionality...");
        Lock testLock = new ReentrantLock();
        
        LockUtils.LockStat lockStat = LockUtils.lock(testLock);
        
        assert lockStat != null : "LockStat should not be null";
        assert lockStat.getLock() == testLock : "Lock should match";
        assert lockStat.isLocked() : "Lock status should be true";
        
        // Test in a separate thread to verify lock is actually held
        final boolean[] threadResult = {true};
        Thread testThread = new Thread(() -> {
            threadResult[0] = testLock.tryLock();
            if (threadResult[0]) {
                testLock.unlock();
            }
        });
        testThread.start();
        testThread.join();
        
        assert !threadResult[0] : "Lock should be occupied by main thread";
        
        lockStat.close();
        
        // Verify lock is released
        assert testLock.tryLock() : "Lock should be released";
        testLock.unlock();
        
        System.out.println("   Basic lock test passed");
    }
    
    public void testTryLockSuccess() throws Exception {
        System.out.println("\n2. Testing tryLock success scenario...");
        Lock testLock = new ReentrantLock();
        
        LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 1L, TimeUnit.SECONDS);
        
        assert lockStat != null : "LockStat should not be null";
        assert lockStat.isLocked() : "Lock status should be true";
        
        lockStat.close();
        
        System.out.println("   TryLock success test passed");
    }
    
    public void testTryLockFailure() throws Exception {
        System.out.println("\n3. Testing tryLock failure scenario...");
        Lock testLock = new ReentrantLock();
        
        testLock.lock();
        
        try {
            LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 10L, TimeUnit.MILLISECONDS);
            
            assert lockStat != null : "LockStat should not be null";
            assert !lockStat.isLocked() : "Lock status should be false";
            
            lockStat.close();
            
        } finally {
            testLock.unlock();
        }
        
        System.out.println("   TryLock failure test passed");
    }
    
    public void testAutoClose() throws Exception {
        System.out.println("\n4. Testing auto-close functionality...");
        Lock testLock = new ReentrantLock();
        
        try (LockUtils.LockStat lockStat = LockUtils.lock(testLock)) {
            assert lockStat.isLocked() : "Lock status should be true";
            
            // Test in a separate thread
            final boolean[] threadResult = {true};
            Thread testThread = new Thread(() -> {
                threadResult[0] = testLock.tryLock();
                if (threadResult[0]) {
                    testLock.unlock();
                }
            });
            testThread.start();
            testThread.join();
            
            assert !threadResult[0] : "Lock should be occupied";
        }
        
        assert testLock.tryLock() : "Lock should be auto-released";
        testLock.unlock();
        
        System.out.println("   Auto-close test passed");
    }
} 
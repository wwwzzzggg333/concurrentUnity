package com.laowang.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.laowang.concurrent.util.LockUtils;

public class QuickTest {
    public static void main(String[] args) {
        System.out.println("Testing LockUtils...");
        
        try {
            // Test 1: Basic lock
            Lock lock1 = new ReentrantLock();
            LockUtils.LockStat stat1 = LockUtils.lock(lock1);
            System.out.println("Test 1 - Lock acquired: " + stat1.isLocked());
            stat1.close();
            System.out.println("Test 1 - Lock released");
            
            // Test 2: TryLock success
            Lock lock2 = new ReentrantLock();
            LockUtils.LockStat stat2 = LockUtils.tryLock(lock2, 1L, TimeUnit.SECONDS);
            System.out.println("Test 2 - TryLock success: " + stat2.isLocked());
            stat2.close();
            System.out.println("Test 2 - Lock released");
            
            // Test 3: TryLock failure
            Lock lock3 = new ReentrantLock();
            lock3.lock();
            LockUtils.LockStat stat3 = LockUtils.tryLock(lock3, 10L, TimeUnit.MILLISECONDS);
            System.out.println("Test 3 - TryLock failure: " + !stat3.isLocked());
            stat3.close();
            lock3.unlock();
            System.out.println("Test 3 - Original lock released");
            
            // Test 4: Try-with-resources
            Lock lock4 = new ReentrantLock();
            try (LockUtils.LockStat stat4 = LockUtils.lock(lock4)) {
                System.out.println("Test 4 - Auto-close lock acquired: " + stat4.isLocked());
            }
            System.out.println("Test 4 - Auto-close completed");
            
            System.out.println("\nAll tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
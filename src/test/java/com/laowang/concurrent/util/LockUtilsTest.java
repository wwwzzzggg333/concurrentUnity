package com.laowang.concurrent.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LockUtils 工具类单元测试
 * 
 * 测试覆盖范围：
 * 1. lock() 方法的基本功能
 * 2. tryLock() 方法的成功和失败场景
 * 3. LockStat 内部类的功能
 * 4. 异常处理情况
 * 5. 资源清理功能
 */
@DisplayName("LockUtils 工具类测试")
class LockUtilsTest {

    private Lock testLock;

    @BeforeEach
    void setUp() {
        testLock = new ReentrantLock();
    }

    @Nested
    @DisplayName("lock() 方法测试")
    class LockMethodTest {

        @Test
        @DisplayName("成功获取锁并返回正确的LockStat对象")
        void testLockSuccess() {
            // 测试获取锁
            LockUtils.LockStat lockStat = LockUtils.lock(testLock);
            
            // 验证返回的LockStat对象
            assertNotNull(lockStat, "LockStat对象不应为null");
            assertEquals(testLock, lockStat.getLock(), "Lock对象应该匹配");
            assertTrue(lockStat.isLocked(), "锁状态应该为true");
            
            // 清理资源
            try {
                lockStat.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
        }

        @Test
        @DisplayName("使用try-with-resources自动释放锁")
        void testLockWithTryWithResources() {
            // 使用try-with-resources确保锁被正确释放
            try (LockUtils.LockStat lockStat = LockUtils.lock(testLock)) {
                assertNotNull(lockStat, "LockStat对象不应为null");
                assertTrue(lockStat.isLocked(), "锁状态应该为true");
                
                // 验证锁确实被获取了
                assertFalse(testLock.tryLock(), "锁应该已被占用");
            } catch (Exception e) {
                fail("使用try-with-resources时不应该抛出异常");
            }
            
            // 验证锁已被释放
            assertTrue(testLock.tryLock(), "锁应该已被释放");
            testLock.unlock(); // 清理测试锁
        }
    }

    @Nested
    @DisplayName("tryLock() 方法测试")
    class TryLockMethodTest {

        @Test
        @DisplayName("成功获取锁的情况")
        void testTryLockSuccess() {
            // 测试在锁可用时获取锁
            LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 1L, TimeUnit.SECONDS);
            
            // 验证返回的LockStat对象
            assertNotNull(lockStat, "LockStat对象不应为null");
            assertEquals(testLock, lockStat.getLock(), "Lock对象应该匹配");
            assertTrue(lockStat.isLocked(), "锁状态应该为true");
            
            // 清理资源
            try {
                lockStat.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
        }

        @Test
        @DisplayName("获取锁失败的情况")
        void testTryLockFailure() throws InterruptedException {
            // 先占用锁
            testLock.lock();
            
            try {
                // 尝试获取已被占用的锁
                LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 100L, TimeUnit.MILLISECONDS);
                
                // 验证返回的LockStat对象
                assertNotNull(lockStat, "LockStat对象不应为null");
                assertEquals(testLock, lockStat.getLock(), "Lock对象应该匹配");
                assertFalse(lockStat.isLocked(), "锁状态应该为false");
                
                // 验证close()方法不会尝试释放未获取的锁
                try {
                    lockStat.close();
                } catch (Exception e) {
                    fail("关闭未获取锁的LockStat时不应该抛出异常");
                }
            } finally {
                testLock.unlock(); // 释放测试锁
            }
        }

        @Test
        @DisplayName("tryLock超时参数测试")
        void testTryLockWithDifferentTimeouts() {
            // 测试不同的超时参数
            LockUtils.LockStat lockStat1 = LockUtils.tryLock(testLock, 0L, TimeUnit.MILLISECONDS);
            assertTrue(lockStat1.isLocked(), "立即获取锁应该成功");
            
            try {
                lockStat1.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
            
            // 测试较长超时时间
            LockUtils.LockStat lockStat2 = LockUtils.tryLock(testLock, 5L, TimeUnit.SECONDS);
            assertTrue(lockStat2.isLocked(), "较长超时时间获取锁应该成功");
            
            try {
                lockStat2.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
        }
    }

    @Nested
    @DisplayName("LockStat 内部类测试")
    class LockStatTest {

        @Test
        @DisplayName("已获取锁的LockStat正确释放锁")
        void testLockStatCloseWithLock() throws Exception {
            testLock.lock();
            LockUtils.LockStat lockStat = new LockUtils.LockStat(testLock, true);
            
            // 验证锁被占用
            assertFalse(testLock.tryLock(), "锁应该被占用");
            
            // 关闭LockStat
            lockStat.close();
            
            // 验证锁被释放
            assertTrue(testLock.tryLock(), "锁应该已被释放");
            testLock.unlock(); // 清理测试锁
        }

        @Test
        @DisplayName("未获取锁的LockStat不会释放锁")
        void testLockStatCloseWithoutLock() throws Exception {
            LockUtils.LockStat lockStat = new LockUtils.LockStat(testLock, false);
            
            // 验证锁可以被获取
            assertTrue(testLock.tryLock(), "锁应该可以被获取");
            testLock.unlock();
            
            // 关闭LockStat（应该不会对锁产生影响）
            lockStat.close();
            
            // 验证锁仍然可以被获取
            assertTrue(testLock.tryLock(), "锁应该仍然可以被获取");
            testLock.unlock();
        }

        @Test
        @DisplayName("LockStat的getter方法测试")
        void testLockStatGetters() {
            LockUtils.LockStat lockStat = new LockUtils.LockStat(testLock, true);
            
            assertEquals(testLock, lockStat.getLock(), "getLock()应该返回正确的Lock对象");
            assertTrue(lockStat.isLocked(), "isLocked()应该返回true");
            
            LockUtils.LockStat lockStat2 = new LockUtils.LockStat(testLock, false);
            assertFalse(lockStat2.isLocked(), "isLocked()应该返回false");
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrentTest {

        @Test
        @DisplayName("多线程环境下的锁竞争测试")
        void testConcurrentLockAccess() throws InterruptedException {
            final int threadCount = 10;
            final Thread[] threads = new Thread[threadCount];
            final boolean[] results = new boolean[threadCount];
            
            // 创建多个线程同时尝试获取锁
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
                assertTrue(results[i], "线程 " + i + " 应该成功获取锁");
            }
            
            // 验证最终锁被释放
            assertTrue(testLock.tryLock(), "所有线程完成后锁应该被释放");
            testLock.unlock();
        }

        @Test
        @DisplayName("多线程tryLock竞争测试")
        void testConcurrentTryLock() throws InterruptedException {
            final int threadCount = 5;
            final Thread[] threads = new Thread[threadCount];
            final int[] successCount = {0};
            
            // 先占用锁
            testLock.lock();
            
            try {
                // 创建多个线程同时尝试获取锁
                for (int i = 0; i < threadCount; i++) {
                    threads[i] = new Thread(() -> {
                        LockUtils.LockStat lockStat = LockUtils.tryLock(testLock, 50L, TimeUnit.MILLISECONDS);
                        if (lockStat.isLocked()) {
                            synchronized (successCount) {
                                successCount[0]++;
                            }
                        }
                        try {
                            lockStat.close();
                        } catch (Exception e) {
                            // 忽略关闭异常
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
                
                // 验证没有线程成功获取锁（因为锁被占用）
                assertEquals(0, successCount[0], "所有tryLock操作都应该失败");
                
            } finally {
                testLock.unlock(); // 释放锁
            }
        }
    }

    @Nested
    @DisplayName("边界情况和异常处理测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("空锁对象处理")
        void testNullLock() {
            // 测试传入null锁对象的情况
            assertThrows(NullPointerException.class, () -> {
                LockUtils.lock(null);
            }, "传入null锁对象应该抛出NullPointerException");
            
            assertThrows(NullPointerException.class, () -> {
                LockUtils.tryLock(null, 1L, TimeUnit.SECONDS);
            }, "传入null锁对象应该抛出NullPointerException");
        }

        @Test
        @DisplayName("tryLock时间参数边界测试")
        void testTryLockTimeoutBoundary() {
            // 测试零超时
            LockUtils.LockStat lockStat1 = LockUtils.tryLock(testLock, 0L, TimeUnit.MILLISECONDS);
            assertTrue(lockStat1.isLocked(), "零超时应该立即获取可用锁");
            
            try {
                lockStat1.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
            
            // 测试负数超时（应该被视为零超时）
            LockUtils.LockStat lockStat2 = LockUtils.tryLock(testLock, -1L, TimeUnit.MILLISECONDS);
            assertTrue(lockStat2.isLocked(), "负数超时应该立即获取可用锁");
            
            try {
                lockStat2.close();
            } catch (Exception e) {
                fail("关闭LockStat时不应该抛出异常");
            }
        }
    }
}
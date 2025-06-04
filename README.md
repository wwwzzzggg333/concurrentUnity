# ConcurrentUtility - 并发工具库

## 项目概述

ConcurrentUtility 是一个轻量级的Java并发工具库，旨在简化多线程编程中的常见场景。该库提供了对Java原生并发工具（如CountDownLatch、Semaphore）的优雅封装，让开发者能够以更简洁、更安全的方式进行并发编程。

## 核心特性

- 🚀 **简化并发编程**：隐藏复杂的线程同步细节，提供简洁的API
- 🛡️ **线程安全**：使用ThreadLocal保证线程安全性
- 📦 **零侵入**：无需修改现有代码架构
- 🎯 **轻量级**：最小化依赖，快速集成

## 环境要求

- Java 17+
- Maven 3.6+

## 依赖说明

项目使用以下主要依赖：
- **Lombok**：简化Java代码编写
- **Logback**：日志记录框架
- **JUnit 5**：单元测试框架

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.laowang</groupId>
    <artifactId>concurrentUtility</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基本使用

```java
import com.laowang.concurrent.util.LatchUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Example {
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public void processData() {
        // 提交并行任务
        LatchUtils.submitTask(executor, () -> {
            // 处理任务1
            System.out.println("处理任务1");
        });
        
        LatchUtils.submitTask(executor, () -> {
            // 处理任务2
            System.out.println("处理任务2");
        });
        
        // 等待所有任务完成（最多等待10秒）
        boolean success = LatchUtils.waitFor(10L, TimeUnit.SECONDS);
        
        if (success) {
            System.out.println("所有任务已完成");
        } else {
            System.out.println("任务执行超时");
        }
    }
}
```

## 工具类详细说明

### LatchUtils - 线程同步工具

`LatchUtils` 是对 `CountDownLatch` 的封装，用于等待多个并行任务完成。

#### 核心方法

##### submitTask(Executor executor, Runnable runnable)
提交一个任务到指定的执行器中。

**参数说明：**
- `executor`: 任务执行器
- `runnable`: 要执行的任务

**使用示例：**
```java
LatchUtils.submitTask(executorService, () -> {
    // 您的业务逻辑
    processBusinessLogic();
});
```

##### waitFor(Long timeout, TimeUnit timeUnit)
等待所有已提交的任务完成。

**参数说明：**
- `timeout`: 最大等待时间（可以为null，表示无限等待）
- `timeUnit`: 时间单位（可以为null，默认为秒）

**返回值：**
- `true`: 所有任务在超时时间内完成
- `false`: 等待超时或发生异常

**使用示例：**
```java
// 等待最多5秒
boolean result = LatchUtils.waitFor(5L, TimeUnit.SECONDS);

// 无限等待
boolean result = LatchUtils.waitFor(null, null);
```

#### 完整使用示例

```java
public class DataProcessor {
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    public void processMultipleData(List<Data> dataList) {
        // 为每个数据项提交处理任务
        for (Data data : dataList) {
            LatchUtils.submitTask(executorService, () -> {
                try {
                    processData(data);
                    log.info("数据处理完成: {}", data.getId());
                } catch (Exception e) {
                    log.error("数据处理失败: {}", data.getId(), e);
                }
            });
        }
        
        // 等待所有数据处理完成
        boolean success = LatchUtils.waitFor(30L, TimeUnit.SECONDS);
        
        if (success) {
            log.info("所有数据处理完成");
            // 执行后续操作
            performPostProcessing();
        } else {
            log.warn("数据处理超时，部分任务可能未完成");
        }
    }
}
```

### SemaphoreUtils - 限流控制工具

`SemaphoreUtils` 是对 `Semaphore` 的封装，用于控制并发访问资源的数量。

#### 核心方法

##### submitTask(Executor executor, Runnable after)
提交一个需要限流控制的任务。

**参数说明：**
- `executor`: 任务执行器
- `after`: 获得许可后执行的任务

##### submitTask(Executor executor, Runnable before, Runnable after)
提交一个带前置处理的限流任务。

**参数说明：**
- `executor`: 任务执行器
- `before`: 获得许可前执行的任务（可选）
- `after`: 获得许可后执行的任务

##### start(int maxSemaphore)
启动所有已提交的任务，并设置最大并发数。

**参数说明：**
- `maxSemaphore`: 最大并发许可数

#### 使用示例

```java
public class ResourceManager {
    private ExecutorService executorService = Executors.newFixedThreadPool(20);
    
    public void accessLimitedResource() {
        // 提交多个需要限流的任务
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            
            SemaphoreUtils.submitTask(executorService, 
                () -> {
                    // 前置处理（不受限流影响）
                    log.info("任务{}准备访问受限资源", taskId);
                },
                () -> {
                    // 受限流控制的任务
                    log.info("任务{}正在访问受限资源", taskId);
                    accessExpensiveResource();
                    log.info("任务{}完成资源访问", taskId);
                }
            );
        }
        
        // 启动所有任务，最多允许5个任务同时访问受限资源
        SemaphoreUtils.start(5);
    }
}
```

### LockUtils - 锁管理工具

`LockUtils` 是对 Java `Lock` 接口的封装，提供了更安全、更便捷的锁管理方式，支持 try-with-resources 语法。

#### 核心方法

##### lock(Lock lock)
获取指定的锁，返回一个 `LockStat` 对象。

**参数说明：**
- `lock`: 要获取的锁对象

**返回值：**
- `LockStat`: 锁状态封装对象，实现了 `AutoCloseable` 接口

**使用示例：**
```java
Lock myLock = new ReentrantLock();

// 传统方式
try (LockUtils.LockStat lockStat = LockUtils.lock(myLock)) {
    // 执行需要同步的代码
    performCriticalSection();
} // 锁会自动释放
```

##### tryLock(Lock lock, long timeout, TimeUnit timeUnit)
尝试在指定时间内获取锁。

**参数说明：**
- `lock`: 要获取的锁对象
- `timeout`: 超时时间
- `timeUnit`: 时间单位

**返回值：**
- `LockStat`: 锁状态封装对象，通过 `isLocked()` 方法可以检查是否成功获取锁

**使用示例：**
```java
Lock myLock = new ReentrantLock();

try (LockUtils.LockStat lockStat = LockUtils.tryLock(myLock, 5L, TimeUnit.SECONDS)) {
    if (lockStat.isLocked()) {
        // 成功获取锁，执行业务逻辑
        performCriticalSection();
    } else {
        // 获取锁失败，执行备选方案
        handleLockFailure();
    }
} // 如果获取了锁，会自动释放
```

#### LockStat 内部类

`LockStat` 是锁状态的封装类，提供以下方法：

- `getLock()`: 获取原始锁对象
- `isLocked()`: 检查是否成功获取了锁
- `close()`: 释放锁（如果已获取）

#### 完整使用示例

```java
public class BankAccount {
    private final Lock accountLock = new ReentrantLock();
    private double balance;
    
    public boolean transfer(BankAccount target, double amount) {
        // 使用 LockUtils 安全地获取锁
        try (LockUtils.LockStat lockStat = LockUtils.tryLock(accountLock, 1L, TimeUnit.SECONDS)) {
            if (!lockStat.isLocked()) {
                log.warn("无法获取账户锁，转账失败");
                return false;
            }
            
            if (balance >= amount) {
                balance -= amount;
                target.deposit(amount);
                log.info("转账成功: {}", amount);
                return true;
            } else {
                log.warn("余额不足，转账失败");
                return false;
            }
        } catch (Exception e) {
            log.error("转账过程中发生异常", e);
            return false;
        }
        // 锁会在这里自动释放
    }
    
    public void deposit(double amount) {
        try (LockUtils.LockStat lockStat = LockUtils.lock(accountLock)) {
            balance += amount;
            log.info("存款成功: {}", amount);
        } catch (Exception e) {
            log.error("存款过程中发生异常", e);
        }
    }
}
```

#### 优势特性

1. **自动资源管理**：实现了 `AutoCloseable` 接口，支持 try-with-resources 语法
2. **异常安全**：即使在发生异常的情况下也能正确释放锁
3. **状态检查**：可以方便地检查锁是否成功获取
4. **简化代码**：减少了手动管理锁的样板代码

#### 与传统方式对比

**传统方式：**
```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // 业务逻辑
    performCriticalSection();
} finally {
    lock.unlock(); // 容易忘记或在异常情况下未执行
}
```

**使用 LockUtils：**
```java
Lock lock = new ReentrantLock();
try (LockUtils.LockStat lockStat = LockUtils.lock(lock)) {
    // 业务逻辑
    performCriticalSection();
} // 锁自动释放，无需手动管理
```

## 传统方式 vs ConcurrentUtility 对比

### 使用传统CountDownLatch的方式：

```java
public void traditionalWay(Object param1, Object param2) {
    CountDownLatch countDownLatch = new CountDownLatch(2);
    
    executorService.execute(() -> {
        try {
            log.info("处理参数1");
            // 处理 param1
        } finally {
            countDownLatch.countDown(); // 容易忘记调用
        }
    });

    executorService.execute(() -> {
        try {
            log.info("处理参数2");
            // 处理 param2
        } finally {
            countDownLatch.countDown(); // 重复代码
        }
    });

    try {
        countDownLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        // 异常处理
    }
    
    // 继续执行后续逻辑
}
```

### 使用ConcurrentUtility的方式：

```java
public void modernWay(Object param1, Object param2) {
    LatchUtils.submitTask(executorService, () -> {
        log.info("处理参数1");
        // 处理 param1
    });

    LatchUtils.submitTask(executorService, () -> {
        log.info("处理参数2");
        // 处理 param2
    });

    LatchUtils.waitFor(10L, TimeUnit.SECONDS);
    
    // 继续执行后续逻辑
}
```

## 设计原理

### 线程安全机制
- 使用 `ThreadLocal` 存储任务信息，确保不同线程间的任务隔离
- 自动管理 `CountDownLatch` 和 `Semaphore` 的生命周期

### 内存管理
- 任务执行完成后自动清理 `ThreadLocal` 数据，避免内存泄漏
- 使用 `LinkedList` 作为任务队列，支持动态扩容

### 异常处理
- 内置异常捕获机制，确保线程同步对象正确释放
- 提供详细的日志记录，便于问题排查

## 注意事项

1. **ThreadLocal清理**：工具类会自动清理ThreadLocal数据，但建议在长时间运行的应用中定期检查
2. **执行器管理**：需要自行管理ExecutorService的生命周期，建议在应用关闭时正确关闭
3. **超时设置**：建议根据实际业务场景设置合理的超时时间
4. **异常处理**：在业务逻辑中要妥善处理异常，避免影响其他并行任务

## 构建与测试

### 编译项目
```bash
mvn clean compile
```

### 运行测试
```bash
mvn test
```

### 打包项目
```bash
mvn clean package
```

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 许可证

本项目基于 MIT 许可证开源。详细信息请查看 [LICENSE](LICENSE) 文件。

## 版本历史

- **v1.0-SNAPSHOT**: 初始版本
  - 实现 LatchUtils 工具类
  - 实现 SemaphoreUtils 工具类
  - 提供基础的并发控制功能

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至项目维护者

---

*让并发编程更简单、更安全！*

---

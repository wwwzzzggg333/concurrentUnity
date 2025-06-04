package com.laowang.concurrent.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL一致性监控工具使用示例
 * 
 * 注意：此测试需要实际的PostgreSQL数据库连接才能运行
 * 在实际使用时，请配置正确的数据源
 */
@Slf4j
public class PostgreSQLConsistencyMonitorTest {
    
    /**
     * 模拟数据源配置
     * 实际使用时应该通过Spring Boot或其他方式配置真实的数据源
     */
    private DataSource createMockDataSource() {
        // 这里返回模拟的数据源
        // 实际使用时应该配置真实的PostgreSQL连接
        log.info("创建模拟数据源（实际使用时请配置真实的PostgreSQL连接）");
        return null; // 模拟返回null，实际使用时返回真实DataSource
    }
    
    /**
     * 基本使用示例
     */
    @Test
    void basicUsageExample() {
        log.info("=== PostgreSQL一致性监控基本使用示例 ===");
        
        // 注意：需要真实的数据源才能运行，这里仅作演示
        DataSource dataSource = createMockDataSource();
        if (dataSource == null) {
            log.info("跳过测试：需要配置真实的PostgreSQL数据源");
            return;
        }
        
        // 创建监控器
        PostgreSQLConsistencyMonitor monitor = new PostgreSQLConsistencyMonitor(dataSource);
        
        try {
            // 启动定期监控
            monitor.startMonitoring();
            log.info("监控已启动，将定期检查WAL状态和数据库状态");
            
            // 等待一段时间让监控运行
            Thread.sleep(10000); // 等待10秒
            
            // 检查特定表的一致性
            monitor.checkTableConsistency("public", "user_table");
            
            // 批量检查多个表
            Map<String, String> tables = new HashMap<>();
            tables.put("public", "orders");
            tables.put("public", "products");
            tables.put("inventory", "stock");
            
            monitor.checkMultipleTablesConsistency(tables, 3);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("测试被中断", e);
        } finally {
            // 停止监控
            monitor.shutdown();
        }
    }
    
    /**
     * 实际项目中的完整配置示例
     */
    @Test
    void realWorldConfigurationExample() {
        log.info("=== 实际项目配置示例 ===");
        
        // 在Spring Boot项目中的典型配置方式：
        String configExample = """
            # application.yml配置示例
            spring:
              datasource:
                url: jdbc:postgresql://localhost:5432/your_database
                username: your_username
                password: your_password
                driver-class-name: org.postgresql.Driver
                hikari:
                  maximum-pool-size: 20
                  minimum-idle: 5
                  idle-timeout: 300000
                  max-lifetime: 1800000
            
            # PostgreSQL推荐配置
            postgresql:
              # 数据一致性相关配置
              fsync: on
              synchronous_commit: on
              wal_sync_method: fdatasync
              checkpoint_timeout: 5min
              max_wal_size: 1GB
              min_wal_size: 80MB
              wal_buffers: 16MB
              
              # 监控相关配置
              log_checkpoints: on
              log_connections: on
              log_disconnections: on
              log_lock_waits: on
              
              # 归档配置（生产环境推荐）
              archive_mode: on
              archive_command: 'cp %p /backup/archive/%f'
            """;
        
        log.info("完整配置示例：\n{}", configExample);
        
        // Java代码配置示例
        String javaConfigExample = """
            @Configuration
            @EnableScheduling
            public class DatabaseMonitoringConfig {
                
                @Autowired
                private DataSource dataSource;
                
                @Bean
                public PostgreSQLConsistencyMonitor consistencyMonitor() {
                    PostgreSQLConsistencyMonitor monitor = 
                        new PostgreSQLConsistencyMonitor(dataSource);
                    
                    // 应用启动时自动开始监控
                    monitor.startMonitoring();
                    
                    return monitor;
                }
                
                @PreDestroy
                public void cleanup() {
                    consistencyMonitor().shutdown();
                }
            }
            
            @Service
            public class DatabaseMaintenanceService {
                
                @Autowired
                private PostgreSQLConsistencyMonitor monitor;
                
                // 每日凌晨2点执行完整的一致性检查
                @Scheduled(cron = "0 0 2 * * ?")
                public void dailyConsistencyCheck() {
                    Map<String, String> criticalTables = getCriticalTables();
                    monitor.checkMultipleTablesConsistency(criticalTables, 5);
                }
                
                // 获取需要检查的关键表
                private Map<String, String> getCriticalTables() {
                    Map<String, String> tables = new HashMap<>();
                    tables.put("public", "users");
                    tables.put("public", "orders");
                    tables.put("public", "payments");
                    tables.put("inventory", "products");
                    tables.put("audit", "operation_logs");
                    return tables;
                }
            }
            """;
        
        log.info("Java配置示例：\n{}", javaConfigExample);
    }
    
    /**
     * 错误处理和告警示例
     */
    @Test
    void errorHandlingExample() {
        log.info("=== 错误处理和告警示例 ===");
        
        String alertingExample = """
            // 结合Spring Boot Actuator进行健康检查
            @Component
            public class PostgreSQLHealthIndicator implements HealthIndicator {
                
                @Autowired
                private PostgreSQLConsistencyMonitor monitor;
                
                @Override
                public Health health() {
                    try {
                        // 执行快速健康检查
                        boolean isHealthy = performQuickHealthCheck();
                        
                        if (isHealthy) {
                            return Health.up()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("status", "WAL and data consistency OK")
                                .build();
                        } else {
                            return Health.down()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("status", "Consistency issues detected")
                                .build();
                        }
                    } catch (Exception e) {
                        return Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("error", e.getMessage())
                            .build();
                    }
                }
            }
            
            // 集成告警系统
            @Service
            public class DatabaseAlertService {
                
                public void sendWALGrowthAlert(String from, String to, long growthMB) {
                    String message = String.format(
                        "PostgreSQL WAL异常增长警告：从 %s 增长到 %s，增长量: %dMB", 
                        from, to, growthMB
                    );
                    
                    // 发送到钉钉、微信、邮件等
                    sendToAlertChannel(message, AlertLevel.WARNING);
                }
                
                public void sendConsistencyAlert(String tableName, String issue) {
                    String message = String.format(
                        "数据一致性问题：表 %s 发现问题 - %s", 
                        tableName, issue
                    );
                    
                    sendToAlertChannel(message, AlertLevel.CRITICAL);
                }
            }
            """;
        
        log.info("告警集成示例：\n{}", alertingExample);
    }
    
    /**
     * 性能优化建议示例
     */
    @Test
    void performanceOptimizationExample() {
        log.info("=== 性能优化建议 ===");
        
        String optimizationTips = """
            PostgreSQL数据一致性监控的性能优化建议：
            
            1. 监控频率调优：
               - WAL状态检查：每1-5分钟一次（根据业务重要性调整）
               - 表一致性检查：每日或每周执行（避免影响业务高峰）
               - 数据库状态检查：每5-10分钟一次
            
            2. 连接池配置：
               - 监控任务使用独立的连接池
               - 避免与业务连接池竞争
               - 设置合适的超时和重试机制
            
            3. 并发控制：
               - 使用SemaphoreUtils限制同时进行的检查任务数量
               - 避免在业务高峰期执行大量检查
               - 错峰执行不同类型的检查任务
            
            4. 索引优化：
               - 确保pg_stat_*视图相关的系统表有适当索引
               - 定期更新表统计信息（ANALYZE）
               - 监控慢查询并优化
            
            5. 存储优化：
               - WAL文件存储在高性能SSD上
               - 数据文件和WAL文件分离存储
               - 定期清理归档日志
            
            6. 内存配置：
               - shared_buffers: 系统内存的25%
               - wal_buffers: 16MB（根据写入负载调整）
               - effective_cache_size: 系统内存的75%
            
            7. 监控指标：
               - WAL生成速率
               - Checkpoint频率和耗时
               - 死锁和长事务
               - 表膨胀率
               - 连接数和会话状态
            """;
        
        log.info("性能优化建议：\n{}", optimizationTips);
    }
} 
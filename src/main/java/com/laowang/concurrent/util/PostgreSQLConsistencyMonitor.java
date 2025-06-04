package com.laowang.concurrent.util;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PostgreSQL数据一致性监控工具
 * 结合ConcurrentUtility工具库，提供数据库一致性检查功能
 * 
 * 主要功能：
 * 1. WAL位置监控
 * 2. 数据库状态检查
 * 3. 表级别一致性验证
 * 4. 异常告警
 */
@Slf4j
public class PostgreSQLConsistencyMonitor {
    
    private final DataSource dataSource;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final Map<String, String> lastWalPositions;
    
    // WAL位置变化阈值（MB）
    private static final long WAL_GROWTH_THRESHOLD = 100;
    
    public PostgreSQLConsistencyMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
        this.executorService = Executors.newFixedThreadPool(4);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.lastWalPositions = new HashMap<>();
    }
    
    /**
     * 启动定期监控任务
     */
    public void startMonitoring() {
        log.info("启动PostgreSQL一致性监控...");
        
        // 每分钟检查WAL状态
        scheduledExecutor.scheduleAtFixedRate(
            this::checkWALStatus, 
            0, 60, TimeUnit.SECONDS
        );
        
        // 每5分钟检查数据库整体状态
        scheduledExecutor.scheduleAtFixedRate(
            this::checkDatabaseStatus, 
            0, 300, TimeUnit.SECONDS
        );
    }
    
    /**
     * 检查WAL状态
     */
    private void checkWALStatus() {
        LatchUtils.submitTask(executorService, () -> {
            try (Connection conn = dataSource.getConnection()) {
                // 检查当前WAL位置
                String currentLsn = getCurrentWALPosition(conn);
                log.debug("当前WAL位置: {}", currentLsn);
                
                // 检查WAL增长是否异常
                checkWALGrowth(currentLsn);
                
                // 检查WAL统计信息
                checkWALStats(conn);
                
            } catch (SQLException e) {
                log.error("WAL状态检查失败", e);
            }
        });
        
        // 等待检查任务完成
        boolean success = LatchUtils.waitFor(30L, TimeUnit.SECONDS);
        if (!success) {
            log.warn("WAL状态检查超时");
        }
    }
    
    /**
     * 检查数据库整体状态
     */
    private void checkDatabaseStatus() {
        LatchUtils.submitTask(executorService, () -> {
            try (Connection conn = dataSource.getConnection()) {
                // 检查是否处于恢复模式
                checkRecoveryStatus(conn);
                
                // 检查checkpoint状态
                checkCheckpointStatus(conn);
                
            } catch (SQLException e) {
                log.error("数据库状态检查失败", e);
            }
        });
        
        LatchUtils.waitFor(60L, TimeUnit.SECONDS);
    }
    
    /**
     * 获取当前WAL位置
     */
    private String getCurrentWALPosition(Connection conn) throws SQLException {
        String sql = "SELECT pg_current_wal_lsn()";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        }
    }
    
    /**
     * 检查WAL增长情况
     */
    private void checkWALGrowth(String currentLsn) {
        if (currentLsn == null) return;
        
        String lastLsn = lastWalPositions.get("current");
        if (lastLsn != null) {
            // 计算WAL位置差异（简化计算）
            long growth = calculateLSNDifference(lastLsn, currentLsn);
            
            if (growth > WAL_GROWTH_THRESHOLD) {
                log.warn("WAL增长异常：从 {} 增长到 {}，增长量: {}MB", 
                        lastLsn, currentLsn, growth);
            }
        }
        
        lastWalPositions.put("current", currentLsn);
        lastWalPositions.put("check_time", LocalDateTime.now().toString());
    }
    
    /**
     * 检查WAL统计信息
     */
    private void checkWALStats(Connection conn) throws SQLException {
        String sql = "SELECT * FROM pg_stat_wal";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long walRecords = rs.getLong("wal_records");
                long walBytes = rs.getLong("wal_bytes");
                long walBuffersFull = rs.getLong("wal_buffers_full");
                
                log.debug("WAL统计 - 记录数: {}, 字节数: {}, 缓冲区满次数: {}", 
                         walRecords, walBytes, walBuffersFull);
                
                // 如果缓冲区频繁满，可能需要调整wal_buffers配置
                if (walBuffersFull > 1000) {
                    log.warn("WAL缓冲区频繁满载，建议检查wal_buffers配置");
                }
            }
        }
    }
    
    /**
     * 检查数据库是否处于恢复模式
     */
    private void checkRecoveryStatus(Connection conn) throws SQLException {
        String sql = "SELECT pg_is_in_recovery()";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                boolean inRecovery = rs.getBoolean(1);
                if (inRecovery) {
                    log.warn("数据库当前处于恢复模式");
                } else {
                    log.debug("数据库运行正常，未处于恢复模式");
                }
            }
        }
    }
    
    /**
     * 检查checkpoint状态
     */
    private void checkCheckpointStatus(Connection conn) throws SQLException {
        String sql = "SELECT * FROM pg_control_checkpoint()";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String checkpointLsn = rs.getString("checkpoint_lsn");
                log.debug("最近checkpoint位置: {}", checkpointLsn);
            }
        }
    }
    
    /**
     * 检查指定表的一致性
     */
    public void checkTableConsistency(String schemaName, String tableName) {
        log.info("开始检查表 {}.{} 的一致性", schemaName, tableName);
        
        LatchUtils.submitTask(executorService, () -> {
            try (Connection conn = dataSource.getConnection()) {
                // 检查表统计信息
                checkTableStats(conn, schemaName, tableName);
                
                // 检查表的约束
                checkTableConstraints(conn, schemaName, tableName);
                
                log.info("表 {}.{} 一致性检查完成", schemaName, tableName);
                
            } catch (SQLException e) {
                log.error("表 {}.{} 一致性检查失败", schemaName, tableName, e);
            }
        });
        
        // 等待检查完成
        boolean success = LatchUtils.waitFor(300L, TimeUnit.SECONDS);
        if (!success) {
            log.warn("表一致性检查超时");
        }
    }
    
    /**
     * 检查表统计信息
     */
    private void checkTableStats(Connection conn, String schemaName, String tableName) 
            throws SQLException {
        String sql = """
            SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del, 
                   n_live_tup, n_dead_tup, last_vacuum, last_autovacuum
            FROM pg_stat_user_tables 
            WHERE schemaname = ? AND tablename = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long liveTuples = rs.getLong("n_live_tup");
                    long deadTuples = rs.getLong("n_dead_tup");
                    
                    log.debug("表 {}.{} 统计 - 活跃行数: {}, 死亡行数: {}", 
                             schemaName, tableName, liveTuples, deadTuples);
                    
                    // 如果死亡行数过多，建议执行VACUUM
                    if (deadTuples > liveTuples * 0.2) {
                        log.warn("表 {}.{} 死亡行数过多({}), 建议执行VACUUM", 
                                schemaName, tableName, deadTuples);
                    }
                }
            }
        }
    }
    
    /**
     * 检查表约束
     */
    private void checkTableConstraints(Connection conn, String schemaName, String tableName) 
            throws SQLException {
        String sql = """
            SELECT conname, contype, pg_get_constraintdef(oid) as definition
            FROM pg_constraint 
            WHERE conrelid = (
                SELECT oid FROM pg_class 
                WHERE relname = ? AND relnamespace = (
                    SELECT oid FROM pg_namespace WHERE nspname = ?
                )
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, schemaName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("conname");
                    String constraintType = rs.getString("contype");
                    
                    log.debug("表 {}.{} 约束: {} (类型: {})", 
                             schemaName, tableName, constraintName, constraintType);
                }
            }
        }
    }
    
    /**
     * 批量检查多个表的一致性（使用并发控制）
     */
    public void checkMultipleTablesConsistency(Map<String, String> tables, int maxConcurrency) {
        log.info("开始批量检查 {} 个表的一致性，最大并发数: {}", tables.size(), maxConcurrency);
        
        // 使用SemaphoreUtils控制并发数
        for (Map.Entry<String, String> entry : tables.entrySet()) {
            String schemaName = entry.getKey();
            String tableName = entry.getValue();
            
            SemaphoreUtils.submitTask(executorService, () -> {
                log.info("准备检查表 {}.{}", schemaName, tableName);
            }, () -> {
                checkTableConsistency(schemaName, tableName);
            });
        }
        
        // 启动所有任务，限制并发数
        SemaphoreUtils.start(maxConcurrency);
    }
    
    /**
     * 简化的LSN差异计算（实际项目中需要更精确的实现）
     */
    private long calculateLSNDifference(String lsn1, String lsn2) {
        // 这里是简化实现，实际应该解析LSN格式进行精确计算
        return Math.abs(lsn1.hashCode() - lsn2.hashCode()) / 1024 / 1024;
    }
    
    /**
     * 停止监控并清理资源
     */
    public void shutdown() {
        log.info("停止PostgreSQL一致性监控...");
        
        scheduledExecutor.shutdown();
        executorService.shutdown();
        
        try {
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("监控停止过程中被中断", e);
        }
        
        log.info("PostgreSQL一致性监控已停止");
    }
} 
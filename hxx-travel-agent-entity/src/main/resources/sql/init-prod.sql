-- =========================================
-- hxx-travel-agent 生产环境部署脚本
-- =========================================

-- 1. 创建生产数据库
CREATE DATABASE hxx_travel_agent
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hxx_travel_agent;

-- 2. 创建生产表
CREATE TABLE IF NOT EXISTS trip_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trip_id VARCHAR(100) NOT NULL COMMENT '行程唯一标识',
    destination VARCHAR(100) NOT NULL COMMENT '目的地',
    summary TEXT COMMENT '行程概述',
    itinerary_json LONGTEXT NOT NULL COMMENT '完整行程JSON数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_trip_id (trip_id),
    INDEX idx_destination (destination),
    INDEX idx_update_time (update_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程记录表';

-- 3. 创建只读用户（可选）
-- CREATE USER 'hxx_travel_ro'@'%' IDENTIFIED BY 'readonly_password';
-- GRANT SELECT ON hxx_travel_agent.* TO 'hxx_travel_ro'@'%';

-- 4. 创建读写用户
-- CREATE USER 'hxx_travel_rw'@'%' IDENTIFIED BY 'readwrite_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON hxx_travel_agent.* TO 'hxx_travel_rw'@'%';
-- FLUSH PRIVILEGES;

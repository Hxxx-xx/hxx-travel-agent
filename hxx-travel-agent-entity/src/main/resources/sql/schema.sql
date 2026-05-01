-- =========================================
-- hxx-travel-agent 数据库建表脚本
-- 数据库名: hxx_travel_agent
-- =========================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS hxx_travel_agent
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hxx_travel_agent;

-- =========================================
-- 行程记录表
-- =========================================
DROP TABLE IF EXISTS trip_records;

CREATE TABLE trip_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trip_id VARCHAR(100) NOT NULL COMMENT '行程唯一标识',
    destination VARCHAR(100) NOT NULL COMMENT '目的地',
    summary TEXT COMMENT '行程概述',
    itinerary_json LONGTEXT NOT NULL COMMENT '完整行程JSON数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY uk_trip_id (trip_id),
    INDEX idx_destination (destination),
    INDEX idx_update_time (update_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程记录表';

-- =========================================
-- 行程记录表 - 开发环境
-- =========================================
CREATE DATABASE IF NOT EXISTS hxx_travel_agent_dev
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hxx_travel_agent_dev;

DROP TABLE IF EXISTS trip_records;

CREATE TABLE trip_records (
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

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程记录表-开发环境';

-- =========================================
-- 行程记录表 - 测试环境
-- =========================================
CREATE DATABASE IF NOT EXISTS hxx_travel_agent_test
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hxx_travel_agent_test;

DROP TABLE IF EXISTS trip_records;

CREATE TABLE trip_records (
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

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程记录表-测试环境';

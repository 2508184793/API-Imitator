-- API-Imitator Database Initialization Script (v2)
-- MySQL 8.0+
-- 包含 v2 版本所有新字段

CREATE DATABASE IF NOT EXISTS api_mock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE api_mock;

-- API 配置表
CREATE TABLE IF NOT EXISTS api_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    description VARCHAR(1000),
    -- ========== v2 新增字段 ==========
    status_code INT DEFAULT 200 COMMENT 'HTTP 响应状态码',
    response_headers TEXT COMMENT '响应头（JSON 格式）',
    delay_ms INT DEFAULT 0 COMMENT '响应延迟（毫秒）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
    -- ========== 原有字段 ==========
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_path_method (path, method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 字段配置表
CREATE TABLE IF NOT EXISTS field_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_config_id BIGINT NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    field_value TEXT,
    parent_field_id BIGINT,
    INDEX idx_api_config (api_config_id),
    INDEX idx_parent_field (parent_field_id),
    CONSTRAINT fk_field_api_config FOREIGN KEY (api_config_id) REFERENCES api_config(id) ON DELETE CASCADE,
    CONSTRAINT fk_field_parent FOREIGN KEY (parent_field_id) REFERENCES field_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

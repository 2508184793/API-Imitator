-- =============================================
-- API-Imitator v1 -> v2 数据库升级脚本
-- =============================================
-- 执行方式：
-- 1. 备份数据库（重要！）
-- 2. 在已有数据库中执行此脚本
-- 3. 重启应用
-- =============================================

USE api_mock;

-- 1. 新增 HTTP 状态码字段
ALTER TABLE api_config 
ADD COLUMN status_code INT DEFAULT 200 COMMENT 'HTTP 响应状态码' 
AFTER description;

-- 2. 新增响应头字段
ALTER TABLE api_config 
ADD COLUMN response_headers TEXT COMMENT '响应头（JSON 格式）' 
AFTER status_code;

-- 3. 新增延迟响应字段
ALTER TABLE api_config 
ADD COLUMN delay_ms INT DEFAULT 0 COMMENT '响应延迟（毫秒）' 
AFTER response_headers;

-- 4. 新增启用/禁用开关字段
ALTER TABLE api_config 
ADD COLUMN enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用' 
AFTER delay_ms;

-- =============================================
-- 验证升级结果（执行完后可以查询确认）
-- =============================================
-- DESC api_config;

-- =============================================
-- 预期输出应该包含这 4 个新字段：
-- status_code
-- response_headers  
-- delay_ms
-- enabled
-- =============================================
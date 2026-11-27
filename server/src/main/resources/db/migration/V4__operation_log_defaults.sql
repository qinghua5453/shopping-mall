-- =====================================================================
-- V4__operation_log_defaults.sql
-- 为 sys_operation_log 的 NOT NULL 字符串列补充默认值，防止应用层漏赋值导致插入失败。
-- 注意：params 列为 TEXT，MySQL 不允许 TEXT 列设置默认值，由应用层保证非空。
-- =====================================================================
ALTER TABLE `sys_operation_log`
    ALTER COLUMN `username` SET DEFAULT '',
    ALTER COLUMN `module` SET DEFAULT '',
    ALTER COLUMN `operation` SET DEFAULT '',
    ALTER COLUMN `method` SET DEFAULT '',
    ALTER COLUMN `path` SET DEFAULT '',
    ALTER COLUMN `error_msg` SET DEFAULT '',
    ALTER COLUMN `trace_id` SET DEFAULT '',
    ALTER COLUMN `ip` SET DEFAULT '',
    ALTER COLUMN `user_agent` SET DEFAULT '';

-- =====================================================================
-- V3__rbac_seed_and_operation_log.sql
-- 草稿：补齐按钮/接口级权限种子、角色授权、操作日志表
-- 对应设计文档：docs/07-rbac-design.md
-- 注意：本文件为设计稿，实施前需经评审后再放入 db/migration/
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 操作日志表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `admin_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人ID',
  `username`    VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '操作人登录名',
  `module`      VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '模块（system/coupon/...）',
  `operation`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '操作描述',
  `method`      VARCHAR(10)     NOT NULL DEFAULT '' COMMENT 'HTTP方法',
  `path`        VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '请求路径',
  `params`      TEXT            NULL COMMENT '请求参数摘要（已脱敏）',
  `success`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `error_msg`   VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '失败原因',
  `cost_ms`     INT             NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  `trace_id`    VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '链路ID',
  `ip`          VARCHAR(45)     NOT NULL DEFAULT '' COMMENT '客户端IP',
  `user_agent`  VARCHAR(512)    NOT NULL DEFAULT '' COMMENT 'User-Agent',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_created` (`admin_id`, `created_at`),
  KEY `idx_module_created` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志';

-- ---------------------------------------------------------------------
-- 2. 按钮/接口级权限种子（permission_type=3）
--    父节点为 V1 已建的菜单：110=管理员 120=角色 130=权限 140=日志
--    310=券模板 320=券码 330=用户券 340=叠加规则
--    code 一经上线不可改名
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_permission`
(`id`, `parent_id`, `permission_type`, `code`, `name`, `api_method`, `api_path`, `sort`, `visible`, `status`)
VALUES
-- 管理员管理 (parent=110)
(111, 110, 3, 'system:admin:create',        '新建管理员',   'POST',   '/api/v1/admin/system/admins',                 1, 0, 1),
(112, 110, 3, 'system:admin:update',        '编辑管理员',   'PUT',    '/api/v1/admin/system/admins/*',               2, 0, 1),
(113, 110, 3, 'system:admin:delete',        '删除管理员',   'DELETE', '/api/v1/admin/system/admins/*',               3, 0, 1),
(114, 110, 3, 'system:admin:assign-role',   '分配角色',     'POST',   '/api/v1/admin/system/admins/*/roles',         4, 0, 1),
(115, 110, 3, 'system:admin:reset-password','重置密码',     'POST',   '/api/v1/admin/system/admins/*/reset-password',5, 0, 1),
-- 角色管理 (parent=120)
(121, 120, 3, 'system:role:create',           '新建角色',   'POST',   '/api/v1/admin/system/roles',                  1, 0, 1),
(122, 120, 3, 'system:role:update',           '编辑角色',   'PUT',    '/api/v1/admin/system/roles/*',                2, 0, 1),
(123, 120, 3, 'system:role:delete',           '删除角色',   'DELETE', '/api/v1/admin/system/roles/*',                3, 0, 1),
(124, 120, 3, 'system:role:assign-permission','分配权限',   'POST',   '/api/v1/admin/system/roles/*/permissions',    4, 0, 1),
-- 权限管理 (parent=130)
(131, 130, 3, 'system:permission:update',     '编辑权限',   'PUT',    '/api/v1/admin/system/permissions/*',          1, 0, 1),
-- 操作日志 (parent=140) —— view 复用菜单 140，无需按钮
-- 券模板 (parent=310)
(311, 310, 3, 'coupon:template:create',       '新建模板',   'POST',   '/api/v1/admin/coupons/templates',             1, 0, 1),
(312, 310, 3, 'coupon:template:update',       '编辑模板',   'PUT',    '/api/v1/admin/coupons/templates/*',           2, 0, 1),
(313, 310, 3, 'coupon:template:delete',       '删除模板',   'DELETE', '/api/v1/admin/coupons/templates/*',           3, 0, 1),
(314, 310, 3, 'coupon:template:publish',      '发布模板',   'POST',   '/api/v1/admin/coupons/templates/*/publish',   4, 0, 1),
(315, 310, 3, 'coupon:template:offline',      '下线模板',   'POST',   '/api/v1/admin/coupons/templates/*/offline',   5, 0, 1),
-- 券码 (parent=320)
(321, 320, 3, 'coupon:code:generate',         '生成券码',   'POST',   '/api/v1/admin/coupons/codes/generate',        1, 0, 1),
(322, 320, 3, 'coupon:code:void',             '作废券码',   'POST',   '/api/v1/admin/coupons/codes/void',            2, 0, 1),
-- 用户券 (parent=330)
(331, 330, 3, 'coupon:instance:grant',        '定向发券',   'POST',   '/api/v1/admin/coupons/instances/grant',       1, 0, 1),
(332, 330, 3, 'coupon:instance:freeze',       '冻结券',     'POST',   '/api/v1/admin/coupons/instances/*/freeze',    2, 0, 1),
(333, 330, 3, 'coupon:instance:unfreeze',     '解冻券',     'POST',   '/api/v1/admin/coupons/instances/*/unfreeze',  3, 0, 1),
-- 叠加规则 (parent=340)
(341, 340, 3, 'coupon:rule:upsert',           '编辑规则',   'POST',   '/api/v1/admin/coupons/stack-rules',           1, 0, 1),
(342, 340, 3, 'coupon:rule:delete',           '删除规则',   'DELETE', '/api/v1/admin/coupons/stack-rules',           2, 0, 1);

-- ---------------------------------------------------------------------
-- 3. 内置角色：运营管理员（id=2），拥有优惠券模块全部权限
--    SUPER_ADMIN(id=1) 已在 V1 中创建
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `description`, `data_scope`, `status`, `sort`)
VALUES (2, 'OPERATOR', '运营管理员', '内置运营角色，拥有优惠券模块权限', 1, 1, 10);

-- ---------------------------------------------------------------------
-- 4. SUPER_ADMIN 全量授权（所有 status=1 的权限）
--    SUPER_ADMIN 鉴权走短路，此处授权用于角色详情展示与一致性
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission` WHERE `status` = 1;

-- ---------------------------------------------------------------------
-- 5. 运营管理员(OPERATOR) 授权：优惠券模块（parent=300 及其子树）+ 目录300
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM `sys_permission`
WHERE `status` = 1
  AND (`id` = 300 OR `parent_id` = 300
       OR `parent_id` IN (310, 320, 330, 340));

CREATE TABLE `sys_admin` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
  `email` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `pwd_reset_required` TINYINT NOT NULL DEFAULT 1 COMMENT '1首次登录需改密',
  `last_login_at` DATETIME(3) NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员';

CREATE TABLE `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
  `data_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '1全部 2仅本人',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

CREATE TABLE `sys_permission` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父ID',
  `permission_type` TINYINT NOT NULL COMMENT '1目录 2菜单 3按钮 4接口',
  `code` VARCHAR(100) NOT NULL COMMENT '权限码',
  `name` VARCHAR(64) NOT NULL COMMENT '名称',
  `route_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '前端路由',
  `component` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '前端组件',
  `api_method` VARCHAR(10) NOT NULL DEFAULT '' COMMENT 'HTTP方法',
  `api_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '接口路径',
  `icon` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '图标',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '1可见 0隐藏',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限菜单';

CREATE TABLE `sys_admin_role` (
  `admin_id` BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`admin_id`, `role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员角色';

CREATE TABLE `sys_role_permission` (
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限';

CREATE TABLE `sys_admin_login_log` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '日志ID',
  `admin_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '管理员ID',
  `username` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录名',
  `device_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备标识',
  `client_type` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '客户端类型',
  `ip` VARCHAR(45) NOT NULL DEFAULT '' COMMENT '客户端IP',
  `user_agent` VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'User-Agent',
  `success` TINYINT NOT NULL COMMENT '1成功 0失败',
  `fail_reason` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '失败原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_created` (`admin_id`, `created_at`),
  KEY `idx_ip_created` (`ip`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员登录日志';

INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `data_scope`, `status`, `sort`)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '内置超级管理员，拥有全部权限', 1, 1, 0);

INSERT INTO `sys_permission`
(`id`, `parent_id`, `permission_type`, `code`, `name`, `route_path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
(100, 0, 1, 'system', '系统管理', '/system', '', 'setting', 100, 1, 1),
(110, 100, 2, 'system:admin:view', '管理员管理', '/system/admins', 'system/admins', 'user', 110, 1, 1),
(120, 100, 2, 'system:role:view', '角色管理', '/system/roles', 'system/roles', 'role', 120, 1, 1),
(130, 100, 2, 'system:permission:view', '权限管理', '/system/permissions', 'system/permissions', 'permission', 130, 1, 1),
(140, 100, 2, 'system:log:view', '操作日志', '/system/logs', 'system/logs', 'log', 140, 1, 1),
(200, 0, 1, 'member', '会员管理', '/member', '', 'member', 200, 1, 1),
(210, 200, 2, 'member:member:view', '会员列表', '/member/list', 'member/list', 'list', 210, 1, 1),
(300, 0, 1, 'coupon', '优惠券管理', '/coupon', '', 'coupon', 300, 1, 1),
(310, 300, 2, 'coupon:template:view', '券模板', '/coupon/templates', 'coupon/templates', 'template', 310, 1, 1),
(320, 300, 2, 'coupon:code:view', '券码管理', '/coupon/codes', 'coupon/codes', 'code', 320, 1, 1),
(330, 300, 2, 'coupon:instance:view', '用户券', '/coupon/instances', 'coupon/instances', 'instance', 330, 1, 1),
(340, 300, 2, 'coupon:rule:view', '叠加规则', '/coupon/rules', 'coupon/rules', 'rule', 340, 1, 1);

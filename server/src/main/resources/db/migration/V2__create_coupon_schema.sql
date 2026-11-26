-- =============================================================
-- 优惠券域表结构（MySQL 8.0）
-- 状态：设计稿，待确认后移动到 src/main/resources/db/migration/
-- 与 docs/02-database.md 第 5 节保持一致，差异见 docs/06-coupon-design.md
-- =============================================================

-- 1. 券模板：同一套规则和库存
CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_no`         VARCHAR(32)     NOT NULL COMMENT '模板编号，对外展示',
  `name`                VARCHAR(128)    NOT NULL COMMENT '券名称',
  `coupon_type`         TINYINT         NOT NULL COMMENT '1满减 2折扣 3无门槛',
  `scope_type`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1全场 2类目 3SPU 4SKU',
  `scope_values`        JSON            NULL COMMENT '适用范围：{"include":[],"exclude":[]}',
  `threshold_amount`    BIGINT          NOT NULL DEFAULT 0 COMMENT '使用门槛（分），0为无门槛',
  `discount_amount`     BIGINT          NOT NULL DEFAULT 0 COMMENT '满减/无门槛券优惠金额（分）',
  `discount_rate`       DECIMAL(4,2)    NULL COMMENT '折扣率，8.50表示8.5折',
  `max_discount_amount` BIGINT          NOT NULL DEFAULT 0 COMMENT '折扣券封顶（分），0不封顶',
  `total_quantity`      INT             NOT NULL DEFAULT 0 COMMENT '发放总量，0为不限量',
  `issued_quantity`     INT             NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `per_user_limit`      INT             NOT NULL DEFAULT 1 COMMENT '每人限领',
  `validity_type`       TINYINT         NOT NULL COMMENT '1固定区间 2领取后N天',
  `valid_start`         DATETIME(3)     NULL COMMENT '固定有效期开始',
  `valid_end`           DATETIME(3)     NULL COMMENT '固定有效期结束',
  `valid_days`          INT             NOT NULL DEFAULT 0 COMMENT '领取后有效天数',
  `receive_start`       DATETIME(3)     NULL COMMENT '领取开始时间',
  `receive_end`         DATETIME(3)     NULL COMMENT '领取结束时间',
  `stack_group`         VARCHAR(32)     NOT NULL DEFAULT 'PLATFORM' COMMENT '叠加组，同组互斥',
  `stackable`           TINYINT         NOT NULL DEFAULT 1 COMMENT '1可与其他组叠加 0不可',
  `refund_policy`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1退款退券 2不退',
  `receive_channels`    VARCHAR(64)     NOT NULL DEFAULT 'ALL' COMMENT '可领取渠道，逗号分隔',
  `sort_weight`         INT             NOT NULL DEFAULT 0 COMMENT '券中心排序权重，越大越前',
  `status`              TINYINT         NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已下线',
  `remark`              VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注',
  `version`             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_by`          BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  `updated_by`          BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_no` (`template_no`),
  KEY `idx_status_receive` (`status`, `receive_start`, `receive_end`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板';

-- 2. 用户券实例：某一会员持有的一张券
CREATE TABLE IF NOT EXISTS `coupon_instance` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '券实例ID',
  `coupon_no`       VARCHAR(32)     NOT NULL COMMENT '券号，对外展示',
  `template_id`     BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  `member_id`       BIGINT UNSIGNED NOT NULL COMMENT '持有人ID',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1未使用 2锁定 3已使用 4已过期 5已冻结',
  `source`          VARCHAR(20)     NOT NULL DEFAULT 'CLAIM' COMMENT 'CLAIM/CODE/ADMIN',
  `receive_channel` VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '领取渠道',
  `receive_time`    DATETIME(3)     NOT NULL COMMENT '领取时间',
  `valid_start`     DATETIME(3)     NOT NULL COMMENT '有效期开始',
  `valid_end`       DATETIME(3)     NOT NULL COMMENT '有效期结束',
  `used_time`       DATETIME(3)     NULL COMMENT '使用时间',
  `order_no`        VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '核销订单号',
  `lock_order_no`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '当前锁定订单号',
  `discount_amount` BIGINT          NOT NULL DEFAULT 0 COMMENT '实际优惠金额（分）',
  `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_no` (`coupon_no`),
  KEY `idx_member_status_end` (`member_id`, `status`, `valid_end`),
  KEY `idx_template` (`template_id`),
  KEY `idx_status_end` (`status`, `valid_end`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_lock_order_no` (`lock_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券实例';

-- 3. 券码批次：一次批量生成对应一条
CREATE TABLE IF NOT EXISTS `coupon_code_batch` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_no`    VARCHAR(32)     NOT NULL COMMENT '批次号',
  `template_id` BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  `total`       INT             NOT NULL COMMENT '生成本次数量',
  `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1已生成 2生成中 3失败',
  `created_by`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券码批次';

-- 4. 券码：可兑换的凭证
CREATE TABLE IF NOT EXISTS `coupon_code` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '券码ID',
  `code`        VARCHAR(32)     NOT NULL COMMENT '券码，16位大写字母数字',
  `batch_id`    BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
  `template_id` BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1未使用 2已使用 3已作废',
  `member_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '兑换会员ID',
  `used_time`   DATETIME(3)     NULL COMMENT '兑换时间',
  `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_batch_status` (`batch_id`, `status`),
  KEY `idx_template_status` (`template_id`, `status`),
  KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券码';

-- 5. 领取记录：幂等与审计
CREATE TABLE IF NOT EXISTS `coupon_receive_record` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `request_no`   VARCHAR(64)     NOT NULL COMMENT '幂等键（Idempotency-Key）',
  `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `template_id`  BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  `channel`      VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '渠道',
  `source`       VARCHAR(20)     NOT NULL DEFAULT 'CLAIM' COMMENT 'CLAIM/CODE/ADMIN',
  `success`      TINYINT         NOT NULL COMMENT '1成功 0失败',
  `fail_reason`  VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '失败原因',
  `coupon_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功时的券实例ID',
  `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_no` (`request_no`),
  KEY `idx_member_template` (`member_id`, `template_id`, `created_at`),
  KEY `idx_template_created` (`template_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领券记录';

-- 6. 使用流水：锁券、核销、释放、过期、退券、冻结、解冻
--    order_no 允许为 NULL：
--    - 订单动作（锁券/核销/释放/退券）靠唯一键保证幂等；
--    - 非订单动作（过期/冻结/解冻）可重复记录。
CREATE TABLE IF NOT EXISTS `coupon_use_record` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `coupon_id`       BIGINT UNSIGNED NOT NULL COMMENT '券实例ID',
  `coupon_no`       VARCHAR(32)     NOT NULL COMMENT '券号',
  `member_id`       BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `template_id`     BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  `order_no`        VARCHAR(64)     NULL COMMENT '订单号，非订单动作为 NULL',
  `action`          TINYINT         NOT NULL COMMENT '1锁券 2核销 3释放 4过期 5退券 6冻结 7解冻',
  `before_status`   TINYINT         NOT NULL COMMENT '变更前状态',
  `after_status`    TINYINT         NOT NULL COMMENT '变更后状态',
  `discount_amount` BIGINT          NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
  `operator_type`   VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT 'MEMBER/ADMIN/SYSTEM',
  `operator_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人ID',
  `remark`          VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注/失败原因',
  `trace_id`        VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '链路ID',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_action_order` (`coupon_id`, `action`, `order_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_member_created` (`member_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券使用流水';

-- 7. 叠加规则：未配置的组合默认互斥
CREATE TABLE IF NOT EXISTS `coupon_stack_rule` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_a`         VARCHAR(32)     NOT NULL COMMENT '叠加组A',
  `group_b`         VARCHAR(32)     NOT NULL COMMENT '叠加组B，字典序保证唯一',
  `allow_stack`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1可叠加 0互斥',
  `max_stack_count` TINYINT         NOT NULL DEFAULT 3 COMMENT '该组合下最大叠加张数',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_groups` (`group_a`, `group_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券叠加规则';

-- =============================================================
-- 初始化数据
-- =============================================================

-- 叠加规则示例：同组仍然互斥；跨组按本表判断
INSERT INTO `coupon_stack_rule` (`group_a`, `group_b`, `allow_stack`, `max_stack_count`, `status`)
VALUES
  ('CATEGORY', 'MERCHANT', 0, 1, 1),
  ('CATEGORY', 'PLATFORM', 1, 2, 1),
  ('MERCHANT', 'PLATFORM', 1, 2, 1)
ON DUPLICATE KEY UPDATE
  `allow_stack` = VALUES(`allow_stack`),
  `max_stack_count` = VALUES(`max_stack_count`),
  `status` = VALUES(`status`);

-- 按钮级权限种子
-- V1 已经写入 coupon 目录及 4 个菜单，这里只补按钮，parent_id 指向对应菜单。
-- 按钮 permission_type=3、visible=0，不会出现在菜单树中。
INSERT INTO `sys_permission`
(`id`, `parent_id`, `permission_type`, `code`, `name`, `route_path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
  (311, 310, 3, 'coupon:template:create',  '新建券模板', '', '', '', 0, 0, 1),
  (312, 310, 3, 'coupon:template:update',  '修改券模板', '', '', '', 0, 0, 1),
  (313, 310, 3, 'coupon:template:delete',  '删除券模板', '', '', '', 0, 0, 1),
  (314, 310, 3, 'coupon:template:publish', '发布券模板', '', '', '', 0, 0, 1),
  (315, 310, 3, 'coupon:template:offline', '下线券模板', '', '', '', 0, 0, 1),
  (321, 320, 3, 'coupon:code:generate',    '生成券码',   '', '', '', 0, 0, 1),
  (322, 320, 3, 'coupon:code:void',        '作废券码',   '', '', '', 0, 0, 1),
  (331, 330, 3, 'coupon:instance:freeze',  '冻结用户券', '', '', '', 0, 0, 1),
  (332, 330, 3, 'coupon:instance:unfreeze','解冻用户券', '', '', '', 0, 0, 1),
  (341, 340, 3, 'coupon:rule:update',      '更新叠加规则','', '', '', 0, 0, 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`);

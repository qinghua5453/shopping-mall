package com.shopmall.security.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_admin_login_log")
public class SysAdminLoginLogEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long adminId;
    private String username;
    private String deviceId;
    private String clientType;
    private String ip;
    private String userAgent;
    private Integer success;
    private String failReason;
    private LocalDateTime createdAt;
}

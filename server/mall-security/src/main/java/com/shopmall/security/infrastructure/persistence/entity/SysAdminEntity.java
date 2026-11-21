package com.shopmall.security.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_admin")
public class SysAdminEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String email;
    private Integer status;
    private Integer pwdResetRequired;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

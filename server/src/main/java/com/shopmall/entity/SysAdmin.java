package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysAdmin {
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
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

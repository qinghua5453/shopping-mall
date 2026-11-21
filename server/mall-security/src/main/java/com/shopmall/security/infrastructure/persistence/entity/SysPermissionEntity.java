package com.shopmall.security.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermissionEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private Integer permissionType;
    private String code;
    private String name;
    private String routePath;
    private String component;
    private String apiMethod;
    private String apiPath;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

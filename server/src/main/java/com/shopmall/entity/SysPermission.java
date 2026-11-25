package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {
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

package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer dataScope;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

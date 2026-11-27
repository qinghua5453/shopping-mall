package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperationLog {
    private Long id;
    private Long adminId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String path;
    private String params;
    private Integer success;
    private String errorMsg;
    private Integer costMs;
    private String traceId;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}

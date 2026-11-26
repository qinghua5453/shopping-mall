package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponCodeBatch {
    private Long id;
    private String batchNo;
    private Long templateId;
    private Integer total;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

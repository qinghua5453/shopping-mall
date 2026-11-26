package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponCode {
    private Long id;
    private String code;
    private Long batchId;
    private Long templateId;
    private Integer status;
    private Long memberId;
    private LocalDateTime usedTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

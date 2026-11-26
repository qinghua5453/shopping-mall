package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponUseRecord {
    private Long id;
    private Long couponId;
    private String couponNo;
    private Long memberId;
    private Long templateId;
    private String orderNo;
    private Integer action;
    private Integer beforeStatus;
    private Integer afterStatus;
    private Long discountAmount;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private String traceId;
    private LocalDateTime createdAt;
}

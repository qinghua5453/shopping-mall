package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponTemplate {
    private Long id;
    private String templateNo;
    private String name;
    private Integer couponType;
    private Integer scopeType;
    private String scopeValues;
    private Long thresholdAmount;
    private Long discountAmount;
    private Double discountRate;
    private Long maxDiscountAmount;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer perUserLimit;
    private Integer validityType;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer validDays;
    private LocalDateTime receiveStart;
    private LocalDateTime receiveEnd;
    private String stackGroup;
    private Integer stackable;
    private Integer refundPolicy;
    private String receiveChannels;
    private Integer sortWeight;
    private Integer status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

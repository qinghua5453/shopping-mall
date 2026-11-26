package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponInstance {
    private Long id;
    private String couponNo;
    private Long templateId;
    private Long memberId;
    private Integer status;
    private String source;
    private String receiveChannel;
    private LocalDateTime receiveTime;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private LocalDateTime usedTime;
    private String orderNo;
    private String lockOrderNo;
    private Long discountAmount;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

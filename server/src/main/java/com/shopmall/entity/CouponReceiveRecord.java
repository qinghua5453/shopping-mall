package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponReceiveRecord {
    private Long id;
    private String requestNo;
    private Long memberId;
    private Long templateId;
    private String channel;
    private String source;
    private Integer success;
    private String failReason;
    private Long couponId;
    private LocalDateTime createdAt;
}

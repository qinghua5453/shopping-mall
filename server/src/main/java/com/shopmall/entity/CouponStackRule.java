package com.shopmall.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponStackRule {
    private Long id;
    private String groupA;
    private String groupB;
    private Integer allowStack;
    private Integer maxStackCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

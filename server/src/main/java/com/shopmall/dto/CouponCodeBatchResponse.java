package com.shopmall.dto;

import com.shopmall.entity.CouponCodeBatch;

public record CouponCodeBatchResponse(
        Long id, String batchNo, Long templateId, Integer total, Integer status,
        String createdAt
) {
    public static CouponCodeBatchResponse from(CouponCodeBatch b) {
        return new CouponCodeBatchResponse(
                b.getId(), b.getBatchNo(), b.getTemplateId(), b.getTotal(), b.getStatus(),
                b.getCreatedAt() == null ? null : b.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}

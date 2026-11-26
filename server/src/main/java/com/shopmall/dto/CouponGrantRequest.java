package com.shopmall.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CouponGrantRequest(
        @NotNull(message = "模板ID不能为空") Long templateId,
        @NotEmpty(message = "会员列表不能为空")
        @Size(max = 500, message = "单次最多 500 人")
        List<Long> memberIds,
        @Size(max = 255) String reason
) {
}

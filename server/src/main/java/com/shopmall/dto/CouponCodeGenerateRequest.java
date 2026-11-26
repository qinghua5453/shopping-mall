package com.shopmall.dto;

import jakarta.validation.constraints.*;

public record CouponCodeGenerateRequest(
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于 0")
        @Max(value = 5000, message = "单次最多生成 5000 张")
        Integer count
) {
}

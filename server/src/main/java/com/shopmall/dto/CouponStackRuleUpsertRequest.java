package com.shopmall.dto;

import jakarta.validation.constraints.*;

public record CouponStackRuleUpsertRequest(
        @NotBlank(message = "叠加组 A 不能为空") String groupA,
        @NotBlank(message = "叠加组 B 不能为空") String groupB,
        @NotNull(message = "是否允许叠加不能为空") Integer allowStack,
        @Min(value = 1) @Max(value = 3) Integer maxStackCount,
        Integer status
) {
}

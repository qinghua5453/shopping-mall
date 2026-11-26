package com.shopmall.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CouponInstanceFreezeRequest(
        @NotBlank(message = "冻结原因不能为空")
        @Size(max = 255, message = "原因最长 255 位")
        String reason
) {
}

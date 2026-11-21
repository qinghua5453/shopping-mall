package com.shopmall.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRefreshRequest(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken,
        @NotBlank(message = "设备标识不能为空")
        @Size(min = 8, max = 64, message = "设备标识长度应为 8-64 位")
        String deviceId
) {
}

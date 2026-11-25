package com.shopmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名最长 64 位")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码最长 72 位")
        String password,
        @NotBlank(message = "设备标识不能为空")
        @Size(min = 8, max = 64, message = "设备标识长度应为 8-64 位")
        String deviceId
) {
}

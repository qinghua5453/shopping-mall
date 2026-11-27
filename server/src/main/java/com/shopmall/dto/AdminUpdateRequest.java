package com.shopmall.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateRequest(
        @Size(max = 64, message = "昵称过长") String nickname,
        @Size(max = 128, message = "邮箱过长") String email,
        @NotNull(message = "状态不能为空") Integer status
) {
}

package com.shopmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCreateRequest(
        @NotBlank(message = "登录名不能为空")
        @Size(min = 3, max = 64, message = "登录名长度 3-64 位") String username,
        @Size(max = 64, message = "昵称过长") String nickname,
        List<Long> roleIds
) {
}

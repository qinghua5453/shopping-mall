package com.shopmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PermissionUpdateRequest(
        @NotBlank(message = "名称不能为空")
        @Size(max = 64, message = "名称过长") String name,
        @Size(max = 64, message = "图标过长") String icon,
        Integer sort,
        @NotNull(message = "可见性不能为空") Integer visible
) {
}

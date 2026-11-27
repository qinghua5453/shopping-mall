package com.shopmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称过长") String name,
        @Size(max = 255, message = "描述过长") String description,
        Integer dataScope,
        @NotNull(message = "状态不能为空") Integer status,
        Integer sort
) {
}

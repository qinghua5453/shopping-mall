package com.shopmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 64, message = "角色编码过长") String code,
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称过长") String name,
        @Size(max = 255, message = "描述过长") String description,
        Integer dataScope,
        Integer sort
) {
}

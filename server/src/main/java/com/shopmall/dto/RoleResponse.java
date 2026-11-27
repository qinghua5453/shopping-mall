package com.shopmall.dto;

import java.util.List;

public record RoleResponse(
        String id,
        String code,
        String name,
        String description,
        Integer dataScope,
        Integer status,
        Integer sort,
        List<String> permissionIds
) {
}

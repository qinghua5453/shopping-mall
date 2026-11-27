package com.shopmall.dto;

import java.util.List;

public record PermissionNode(
        String id,
        String parentId,
        Integer permissionType,
        String code,
        String name,
        String routePath,
        String component,
        String apiMethod,
        String apiPath,
        String icon,
        Integer sort,
        Integer visible,
        Integer status,
        List<PermissionNode> children
) {
}

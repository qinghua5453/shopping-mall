package com.shopmall.dto;

import java.util.List;

/**
 * 管理员权限上下文（缓存到 Redis 的内容）。
 */
public record PermissionContext(
        boolean superAdmin,
        List<String> roleCodes,
        List<String> permCodes,
        List<AdminMenu> menus
) {
}

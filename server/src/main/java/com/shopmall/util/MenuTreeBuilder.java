package com.shopmall.util;

import com.shopmall.dto.AdminMenu;
import com.shopmall.entity.SysPermission;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 由权限列表构建菜单树：仅保留「目录(1)/菜单(2)」且 visible=1 的节点。
 */
public final class MenuTreeBuilder {

    private MenuTreeBuilder() {
    }

    public static List<AdminMenu> build(List<SysPermission> permissions) {
        Map<Long, List<SysPermission>> byParent = permissions.stream()
                .filter(item -> item.getPermissionType() == 1 || item.getPermissionType() == 2)
                .filter(item -> item.getVisible() != null && item.getVisible() == 1)
                .collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildChildren(0L, byParent);
    }

    private static List<AdminMenu> buildChildren(Long parentId, Map<Long, List<SysPermission>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(item -> new AdminMenu(
                        String.valueOf(item.getId()),
                        item.getName(),
                        item.getCode(),
                        item.getRoutePath(),
                        item.getIcon(),
                        buildChildren(item.getId(), byParent)))
                .toList();
    }
}

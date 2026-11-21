package com.shopmall.security.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopmall.security.api.dto.AdminMenu;
import com.shopmall.security.domain.model.AdminAccess;
import com.shopmall.security.infrastructure.persistence.entity.SysPermissionEntity;
import com.shopmall.security.infrastructure.persistence.mapper.SysPermissionMapper;
import com.shopmall.security.infrastructure.persistence.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    public AdminAccess getAccess(Long adminId) {
        List<String> roleCodes = roleMapper.selectCodesByAdminId(adminId);
        boolean superAdmin = roleCodes.contains("SUPER_ADMIN");
        List<SysPermissionEntity> permissions = superAdmin
                ? permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionEntity>()
                        .eq(SysPermissionEntity::getStatus, 1)
                        .orderByAsc(SysPermissionEntity::getSort)
                        .orderByAsc(SysPermissionEntity::getId))
                : permissionMapper.selectByAdminId(adminId);

        List<String> permissionCodes = superAdmin
                ? List.of("*")
                : permissions.stream().map(SysPermissionEntity::getCode).distinct().toList();
        return new AdminAccess(permissionCodes, buildMenus(permissions));
    }

    private List<AdminMenu> buildMenus(List<SysPermissionEntity> permissions) {
        Map<Long, List<SysPermissionEntity>> byParent = permissions.stream()
                .filter(item -> item.getPermissionType() == 1 || item.getPermissionType() == 2)
                .filter(item -> item.getVisible() == 1)
                .collect(Collectors.groupingBy(SysPermissionEntity::getParentId));
        return buildChildren(0L, byParent);
    }

    private List<AdminMenu> buildChildren(Long parentId, Map<Long, List<SysPermissionEntity>> byParent) {
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

package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.AssignPermissionsRequest;
import com.shopmall.dto.RoleCreateRequest;
import com.shopmall.dto.RoleResponse;
import com.shopmall.dto.RoleUpdateRequest;
import com.shopmall.entity.SysRole;
import com.shopmall.mapper.SysRoleMapper;
import com.shopmall.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemRoleService {
    public static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

    private final SysRoleMapper roleMapper;
    private final AdminPermissionService permissionService;

    public List<RoleResponse> list() {
        return roleMapper.selectAll().stream()
                .map(r -> toResponse(r, List.of()))
                .toList();
    }

    public RoleResponse detail(Long id) {
        SysRole role = getRole(id);
        List<String> permIds = roleMapper.selectPermissionIds(id).stream()
                .map(String::valueOf).toList();
        return toResponse(role, permIds);
    }

    @Transactional
    public RoleResponse create(RoleCreateRequest req) {
        if (roleMapper.selectByCode(req.code()) != null) {
            throw new BizException(ErrorCode.ROLE_CODE_DUPLICATE);
        }
        SysRole role = new SysRole();
        role.setId(IdGenerator.nextId());
        role.setCode(req.code());
        role.setName(req.name());
        role.setDescription(req.description() == null ? "" : req.description());
        role.setDataScope(req.dataScope() == null ? 1 : req.dataScope());
        role.setStatus(1);
        role.setSort(req.sort() == null ? 0 : req.sort());
        roleMapper.insert(role);
        permissionService.invalidateAll();
        return detail(role.getId());
    }

    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest req) {
        SysRole role = getRole(id);
        if (SUPER_ADMIN_CODE.equals(role.getCode()) && req.status() != null && req.status() != 1) {
            throw new BizException(ErrorCode.ROLE_BUILTIN_LOCKED);
        }
        role.setName(req.name());
        role.setDescription(req.description() == null ? "" : req.description());
        role.setDataScope(req.dataScope() == null ? role.getDataScope() : req.dataScope());
        role.setStatus(req.status());
        role.setSort(req.sort() == null ? role.getSort() : req.sort());
        roleMapper.update(role);
        permissionService.invalidateAll();
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = getRole(id);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.ROLE_BUILTIN_LOCKED);
        }
        if (roleMapper.countAdminsByRole(id) > 0) {
            throw new BizException(ErrorCode.ROLE_HAS_ADMINS);
        }
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteAdminRolesByRole(id);
        roleMapper.deleteById(id);
        permissionService.invalidateAll();
    }

    @Transactional
    public void assignPermissions(Long id, AssignPermissionsRequest req) {
        getRole(id);
        roleMapper.deleteRolePermissions(id);
        if (req.permissionIds() != null && !req.permissionIds().isEmpty()) {
            roleMapper.insertRolePermissions(id, req.permissionIds());
        }
        permissionService.invalidateAll();
    }

    private SysRole getRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private RoleResponse toResponse(SysRole role, List<String> permissionIds) {
        return new RoleResponse(
                String.valueOf(role.getId()), role.getCode(), role.getName(), role.getDescription(),
                role.getDataScope(), role.getStatus(), role.getSort(), permissionIds);
    }
}

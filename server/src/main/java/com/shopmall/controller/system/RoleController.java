package com.shopmall.controller.system;

import com.shopmall.annotation.OperationLog;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.AssignPermissionsRequest;
import com.shopmall.dto.RoleCreateRequest;
import com.shopmall.dto.RoleResponse;
import com.shopmall.dto.RoleUpdateRequest;
import com.shopmall.service.SystemRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system/roles")
@RequiredArgsConstructor
public class RoleController {
    private final SystemRoleService roleService;

    @GetMapping
    @RequirePermission("system:role:view")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(roleService.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("system:role:view")
    public ApiResponse<RoleResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(roleService.detail(id));
    }

    @PostMapping
    @RequirePermission("system:role:create")
    @OperationLog(module = "system", operation = "新建角色")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest req) {
        return ApiResponse.success(roleService.create(req));
    }

    @PutMapping("/{id}")
    @RequirePermission("system:role:update")
    @OperationLog(module = "system", operation = "编辑角色")
    public ApiResponse<RoleResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody RoleUpdateRequest req) {
        return ApiResponse.success(roleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:role:delete")
    @OperationLog(module = "system", operation = "删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/permissions")
    @RequirePermission("system:role:assign-permission")
    @OperationLog(module = "system", operation = "分配角色权限")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                               @Valid @RequestBody AssignPermissionsRequest req) {
        roleService.assignPermissions(id, req);
        return ApiResponse.success(null);
    }
}

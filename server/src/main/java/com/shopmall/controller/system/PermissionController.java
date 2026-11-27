package com.shopmall.controller.system;

import com.shopmall.annotation.Logical;
import com.shopmall.annotation.OperationLog;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.PermissionNode;
import com.shopmall.dto.PermissionUpdateRequest;
import com.shopmall.service.SystemPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final SystemPermissionService permissionService;

    @GetMapping
    @RequirePermission(value = {"system:permission:view", "system:role:view"}, logical = Logical.OR)
    public ApiResponse<List<PermissionNode>> tree() {
        return ApiResponse.success(permissionService.tree());
    }

    @PutMapping("/{id}")
    @RequirePermission("system:permission:update")
    @OperationLog(module = "system", operation = "编辑权限")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody PermissionUpdateRequest req) {
        permissionService.update(id, req);
        return ApiResponse.success(null);
    }
}

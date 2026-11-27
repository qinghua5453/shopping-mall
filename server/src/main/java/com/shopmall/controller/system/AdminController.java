package com.shopmall.controller.system;

import com.shopmall.annotation.OperationLog;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.AdminCreateRequest;
import com.shopmall.dto.AdminCreateResponse;
import com.shopmall.dto.AdminDetailResponse;
import com.shopmall.dto.AdminResponse;
import com.shopmall.dto.AdminUpdateRequest;
import com.shopmall.dto.AssignRolesRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.dto.ResetPasswordResponse;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.service.SystemAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/system/admins")
@RequiredArgsConstructor
public class AdminController {
    private final SystemAdminService adminService;

    @GetMapping
    @RequirePermission("system:admin:view")
    public ApiResponse<PageResult<AdminResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(adminService.page(keyword, status, page, pageSize));
    }

    @GetMapping("/{id}")
    @RequirePermission("system:admin:view")
    public ApiResponse<AdminDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(adminService.detail(id));
    }

    @PostMapping
    @RequirePermission("system:admin:create")
    @OperationLog(module = "system", operation = "新建管理员")
    public ApiResponse<AdminCreateResponse> create(@Valid @RequestBody AdminCreateRequest req,
                                                   HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(adminService.create(req, adminId));
    }

    @PutMapping("/{id}")
    @RequirePermission("system:admin:update")
    @OperationLog(module = "system", operation = "编辑管理员")
    public ApiResponse<AdminDetailResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody AdminUpdateRequest req,
                                                   HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(adminService.update(id, req, adminId));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:admin:delete")
    @OperationLog(module = "system", operation = "删除管理员")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        adminService.delete(id, adminId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/roles")
    @RequirePermission("system:admin:assign-role")
    @OperationLog(module = "system", operation = "分配管理员角色")
    public ApiResponse<Void> assignRoles(@PathVariable Long id,
                                         @Valid @RequestBody AssignRolesRequest req,
                                         HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        adminService.assignRoles(id, req, adminId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission("system:admin:reset-password")
    @OperationLog(module = "system", operation = "重置管理员密码")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.success(adminService.resetPassword(id));
    }
}

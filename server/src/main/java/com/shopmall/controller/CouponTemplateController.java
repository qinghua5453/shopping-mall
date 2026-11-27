package com.shopmall.controller;

import com.shopmall.annotation.OperationLog;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.CouponTemplateCreateRequest;
import com.shopmall.dto.CouponTemplateResponse;
import com.shopmall.dto.PageResult;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.service.CouponTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons/templates")
@RequiredArgsConstructor
public class CouponTemplateController {
    private final CouponTemplateService templateService;

    @GetMapping
    @RequirePermission("coupon:template:view")
    public ApiResponse<PageResult<CouponTemplateResponse>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(templateService.page(status, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    @RequirePermission("coupon:template:view")
    public ApiResponse<CouponTemplateResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(templateService.detail(id));
    }

    @PostMapping
    @RequirePermission("coupon:template:create")
    @OperationLog(module = "coupon", operation = "新建券模板")
    public ApiResponse<CouponTemplateResponse> create(
            @Valid @RequestBody CouponTemplateCreateRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(templateService.create(req, adminId));
    }

    @PutMapping("/{id}")
    @RequirePermission("coupon:template:update")
    @OperationLog(module = "coupon", operation = "编辑券模板")
    public ApiResponse<CouponTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponTemplateCreateRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(templateService.update(id, req, adminId));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("coupon:template:delete")
    @OperationLog(module = "coupon", operation = "删除券模板")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    @RequirePermission("coupon:template:publish")
    @OperationLog(module = "coupon", operation = "发布券模板")
    public ApiResponse<CouponTemplateResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(templateService.publish(id));
    }

    @PostMapping("/{id}/offline")
    @RequirePermission("coupon:template:offline")
    @OperationLog(module = "coupon", operation = "下线券模板")
    public ApiResponse<CouponTemplateResponse> offline(@PathVariable Long id) {
        return ApiResponse.success(templateService.offline(id));
    }
}

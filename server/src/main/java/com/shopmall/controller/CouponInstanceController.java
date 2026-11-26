package com.shopmall.controller;

import com.shopmall.common.ApiResponse;
import com.shopmall.dto.CouponGrantRequest;
import com.shopmall.dto.CouponInstanceFreezeRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.CouponInstance;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.service.CouponInstanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons/instances")
@RequiredArgsConstructor
public class CouponInstanceController {
    private final CouponInstanceService instanceService;

    @GetMapping
    public ApiResponse<PageResult<CouponInstance>> page(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(instanceService.page(memberId, templateId, status, page, pageSize));
    }

    @PostMapping("/{id}/freeze")
    public ApiResponse<CouponInstance> freeze(
            @PathVariable Long id,
            @Valid @RequestBody CouponInstanceFreezeRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(instanceService.freeze(id, req, adminId));
    }

    @PostMapping("/{id}/unfreeze")
    public ApiResponse<CouponInstance> unfreeze(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(instanceService.unfreeze(id, adminId));
    }

    @PostMapping("/grant")
    public ApiResponse<Void> grant(
            @Valid @RequestBody CouponGrantRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        instanceService.grant(req, adminId);
        return ApiResponse.success(null);
    }
}

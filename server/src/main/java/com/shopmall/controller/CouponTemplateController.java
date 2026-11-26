package com.shopmall.controller;

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
    public ApiResponse<PageResult<CouponTemplateResponse>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(templateService.page(status, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<CouponTemplateResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(templateService.detail(id));
    }

    @PostMapping
    public ApiResponse<CouponTemplateResponse> create(
            @Valid @RequestBody CouponTemplateCreateRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(templateService.create(req, adminId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponTemplateCreateRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(templateService.update(id, req, adminId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CouponTemplateResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(templateService.publish(id));
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<CouponTemplateResponse> offline(@PathVariable Long id) {
        return ApiResponse.success(templateService.offline(id));
    }
}

package com.shopmall.controller;

import com.shopmall.common.ApiResponse;
import com.shopmall.dto.CouponCodeBatchResponse;
import com.shopmall.dto.CouponCodeGenerateRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.CouponCode;
import com.shopmall.entity.CouponCodeBatch;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.service.CouponCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons/codes")
@RequiredArgsConstructor
public class CouponCodeController {
    private final CouponCodeService codeService;

    @PostMapping("/generate")
    public ApiResponse<CouponCodeBatchResponse> generate(
            @RequestParam Long templateId,
            @Valid @RequestBody CouponCodeGenerateRequest req,
            HttpServletRequest request
    ) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(codeService.generate(templateId, req, adminId));
    }

    @GetMapping("/batches")
    public ApiResponse<java.util.List<CouponCodeBatch>> batches(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(codeService.listBatches(limit));
    }

    @GetMapping
    public ApiResponse<PageResult<CouponCode>> page(
            @RequestParam Long batchId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(codeService.pageByBatch(batchId, status, page, pageSize));
    }

    @PostMapping("/void")
    public ApiResponse<Void> voidCode(@RequestParam String code) {
        codeService.voidCode(code);
        return ApiResponse.success(null);
    }
}

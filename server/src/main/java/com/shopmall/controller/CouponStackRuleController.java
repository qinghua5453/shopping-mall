package com.shopmall.controller;

import com.shopmall.annotation.OperationLog;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.CouponStackRuleUpsertRequest;
import com.shopmall.entity.CouponStackRule;
import com.shopmall.service.CouponStackRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons/stack-rules")
@RequiredArgsConstructor
public class CouponStackRuleController {
    private final CouponStackRuleService stackRuleService;

    @GetMapping
    @RequirePermission("coupon:rule:view")
    public ApiResponse<List<CouponStackRule>> list() {
        return ApiResponse.success(stackRuleService.list());
    }

    @PostMapping
    @RequirePermission("coupon:rule:upsert")
    @OperationLog(module = "coupon", operation = "编辑叠加规则")
    public ApiResponse<CouponStackRule> upsert(@Valid @RequestBody CouponStackRuleUpsertRequest req) {
        return ApiResponse.success(stackRuleService.upsert(req));
    }

    @DeleteMapping
    @RequirePermission("coupon:rule:delete")
    @OperationLog(module = "coupon", operation = "删除叠加规则")
    public ApiResponse<Void> delete(@RequestParam String groupA, @RequestParam String groupB) {
        stackRuleService.delete(groupA, groupB);
        return ApiResponse.success(null);
    }
}

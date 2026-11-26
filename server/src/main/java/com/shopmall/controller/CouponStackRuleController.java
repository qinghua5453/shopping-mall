package com.shopmall.controller;

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
    public ApiResponse<List<CouponStackRule>> list() {
        return ApiResponse.success(stackRuleService.list());
    }

    @PostMapping
    public ApiResponse<CouponStackRule> upsert(@Valid @RequestBody CouponStackRuleUpsertRequest req) {
        return ApiResponse.success(stackRuleService.upsert(req));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam String groupA, @RequestParam String groupB) {
        stackRuleService.delete(groupA, groupB);
        return ApiResponse.success(null);
    }
}

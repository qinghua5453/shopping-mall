package com.shopmall.controller.system;

import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.dto.OperationLogResponse;
import com.shopmall.dto.PageResult;
import com.shopmall.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/system/logs")
@RequiredArgsConstructor
public class OperationLogController {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogService operationLogService;

    @GetMapping
    @RequirePermission("system:log:view")
    public ApiResponse<PageResult<OperationLogResponse>> page(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        LocalDateTime startDt = parse(start);
        LocalDateTime endDt = parse(end);
        return ApiResponse.success(operationLogService.page(adminId, module, success, startDt, endDt, page, pageSize));
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FMT);
        } catch (Exception e) {
            return null;
        }
    }
}

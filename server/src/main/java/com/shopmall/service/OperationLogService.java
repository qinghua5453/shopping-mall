package com.shopmall.service;

import com.shopmall.dto.OperationLogResponse;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.SysOperationLog;
import com.shopmall.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper logMapper;

    @Async("operationLogExecutor")
    public void saveAsync(SysOperationLog operationLog) {
        try {
            logMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("写入操作日志失败", e);
        }
    }

    public PageResult<OperationLogResponse> page(Long adminId, String module, Integer success,
                                                 LocalDateTime start, LocalDateTime end,
                                                 int page, int pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        long total = logMapper.countPage(adminId, module, success, start, end);
        List<OperationLogResponse> list = logMapper
                .selectPage(adminId, module, success, start, end, offset, pageSize)
                .stream().map(OperationLogService::toResponse).toList();
        return new PageResult<>(page, pageSize, total, list);
    }

    private static OperationLogResponse toResponse(SysOperationLog log) {
        return new OperationLogResponse(
                String.valueOf(log.getId()),
                String.valueOf(log.getAdminId()),
                log.getUsername(),
                log.getModule(),
                log.getOperation(),
                log.getMethod(),
                log.getPath(),
                log.getParams(),
                log.getSuccess(),
                log.getErrorMsg(),
                log.getCostMs(),
                log.getTraceId(),
                log.getIp(),
                log.getUserAgent(),
                log.getCreatedAt());
    }
}

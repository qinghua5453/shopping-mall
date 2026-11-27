package com.shopmall.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.annotation.OperationLog;
import com.shopmall.entity.SysOperationLog;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 操作日志切面：对标注 @OperationLog 的管理端写操作异步记录审计日志。
 * 敏感字段（password/token/authorization）在落库前脱敏。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private static final Pattern SENSITIVE = Pattern.compile(
            "\"((?:old|new)?[Pp]assword|[Tt]oken|[Aa]uthorization|accessToken|refreshToken)\"\\s*:\\s*\"[^\"]*\"");

    private final OperationLogService operationLogService;
    private final SysAdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        Long adminId = request != null
                ? (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE) : null;
        String username = adminId != null ? lookupUsername(adminId) : "";

        SysOperationLog record = new SysOperationLog();
        record.setAdminId(adminId == null ? 0L : adminId);
        record.setUsername(username);
        record.setModule(operationLog.module());
        record.setOperation(operationLog.operation());
        record.setMethod(request != null ? request.getMethod() : "");
        record.setPath(request != null ? request.getRequestURI() : "");
        record.setParams("");
        record.setErrorMsg("");
        record.setIp(request != null ? clientIp(request) : "");
        record.setUserAgent(request != null ? truncate(request.getHeader("User-Agent"), 500) : "");
        record.setTraceId(UUID.randomUUID().toString().replace("-", ""));

        if (operationLog.recordParams()) {
            record.setParams(maskParams(joinPoint.getArgs()));
        }

        Object result;
        try {
            result = joinPoint.proceed();
            record.setSuccess(1);
        } catch (Throwable t) {
            record.setSuccess(0);
            record.setErrorMsg(truncate(t.getClass().getSimpleName() + ": " + t.getMessage(), 500));
            throw t;
        } finally {
            record.setCostMs((int) (System.currentTimeMillis() - start));
            operationLogService.saveAsync(record);
        }
        return result;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String lookupUsername(Long adminId) {
        try {
            var admin = adminMapper.selectById(adminId);
            return admin == null ? "" : admin.getUsername();
        } catch (Exception e) {
            return "";
        }
    }

    private String maskParams(Object[] args) {
        try {
            Object[] safe = Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .toArray();
            String json = objectMapper.writeValueAsString(safe);
            return truncate(SENSITIVE.matcher(json).replaceAll("\"$1\":\"***\""), 2000);
        } catch (Exception e) {
            return "";
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "" : ip;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}

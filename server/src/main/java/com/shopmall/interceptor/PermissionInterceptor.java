package com.shopmall.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.annotation.RequirePermission;
import com.shopmall.common.ApiResponse;
import com.shopmall.common.ErrorCode;
import com.shopmall.service.AdminPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 权限校验拦截器：在 LoginInterceptor 之后执行。
 * 读取方法上的 @RequirePermission，通过 AdminPermissionService 校验。
 * 未标注解的方法默认为「已登录即可访问」。
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final AdminPermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        if (adminId == null) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
        if (!permissionService.hasPermission(adminId, annotation.value(), annotation.logical())) {
            writeError(response, ErrorCode.FORBIDDEN);
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}

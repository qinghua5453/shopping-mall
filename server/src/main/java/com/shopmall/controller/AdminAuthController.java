package com.shopmall.controller;

import com.shopmall.annotation.OperationLog;
import com.shopmall.common.ApiResponse;
import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.ChangePasswordRequest;
import com.shopmall.dto.LoginRequest;
import com.shopmall.dto.LoginResponse;
import com.shopmall.dto.MeResponse;
import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private static final Set<String> SUPPORTED_CLIENT_TYPES = Set.of("PC", "H5");

    private final AdminAuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader("X-Client-Type") String clientType,
            HttpServletRequest servletRequest
    ) {
        if (!SUPPORTED_CLIENT_TYPES.contains(clientType)) {
            throw new BizException(ErrorCode.CLIENT_TYPE_INVALID);
        }
        return ApiResponse.success(authService.login(request, servletRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        return ApiResponse.success(authService.me(adminId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String sessionId = (String) request.getAttribute(LoginInterceptor.SESSION_ID_ATTRIBUTE);
        authService.logout(sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/password")
    @OperationLog(module = "system", operation = "修改自身密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest servletRequest) {
        Long adminId = (Long) servletRequest.getAttribute(LoginInterceptor.ADMIN_ID_ATTRIBUTE);
        authService.changePassword(adminId, request);
        return ApiResponse.success(null);
    }
}

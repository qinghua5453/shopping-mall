package com.shopmall.admin.api.controller;

import com.shopmall.admin.api.dto.AdminLoginRequest;
import com.shopmall.admin.api.dto.AdminRefreshRequest;
import com.shopmall.common.api.ApiResponse;
import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.api.dto.AdminLoginCommand;
import com.shopmall.security.api.dto.AdminMeResponse;
import com.shopmall.security.api.dto.AdminRefreshCommand;
import com.shopmall.security.api.dto.AdminTokenResponse;
import com.shopmall.security.api.facade.AdminAuthFacade;
import com.shopmall.security.domain.model.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private static final Set<String> SUPPORTED_CLIENTS = Set.of("PC", "H5");

    private final AdminAuthFacade adminAuthFacade;

    @PostMapping("/login")
    public ApiResponse<AdminTokenResponse> login(
            @Valid @RequestBody AdminLoginRequest body,
            @RequestHeader("X-Client-Type") String clientType,
            HttpServletRequest request) {
        validateClientType(clientType);
        AdminLoginCommand command = new AdminLoginCommand(
                body.username(),
                body.password(),
                body.deviceId(),
                clientType,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return ApiResponse.success(adminAuthFacade.login(command));
    }

    @PostMapping("/refresh")
    public ApiResponse<AdminTokenResponse> refresh(
            @Valid @RequestBody AdminRefreshRequest body,
            @RequestHeader("X-Client-Type") String clientType) {
        validateClientType(clientType);
        return ApiResponse.success(adminAuthFacade.refresh(
                new AdminRefreshCommand(body.refreshToken(), body.deviceId(), clientType)));
    }

    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(adminAuthFacade.me(principal.adminId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AdminPrincipal principal) {
        adminAuthFacade.logout(principal.adminId(), principal.sessionId());
        return ApiResponse.success(null);
    }

    private void validateClientType(String clientType) {
        if (!SUPPORTED_CLIENTS.contains(clientType)) {
            throw new BizException(ErrorCode.CLIENT_TYPE_INVALID);
        }
    }
}

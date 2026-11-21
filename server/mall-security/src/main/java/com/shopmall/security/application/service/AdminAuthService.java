package com.shopmall.security.application.service;

import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.api.dto.AdminLoginCommand;
import com.shopmall.security.api.dto.AdminMeResponse;
import com.shopmall.security.api.dto.AdminProfile;
import com.shopmall.security.api.dto.AdminRefreshCommand;
import com.shopmall.security.api.dto.AdminTokenResponse;
import com.shopmall.security.api.facade.AdminAuthFacade;
import com.shopmall.security.domain.model.AdminAccess;
import com.shopmall.security.domain.model.AdminSession;
import com.shopmall.security.domain.model.SessionCredential;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService implements AdminAuthFacade {
    private final AdminAccountService accountService;
    private final AdminPermissionService permissionService;
    private final AdminLoginAttemptService loginAttemptService;
    private final AdminSessionService sessionService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminTokenResponse login(AdminLoginCommand command) {
        loginAttemptService.assertAllowed(command.username(), command.ip());
        SysAdminEntity admin = null;
        try {
            admin = accountService.findByUsername(command.username());
            if (admin == null || !passwordEncoder.matches(command.password(), admin.getPassword())) {
                throw new BizException(ErrorCode.BAD_CREDENTIALS);
            }
            if (admin.getStatus() == null || admin.getStatus() != 1) {
                throw new BizException(ErrorCode.ACCOUNT_DISABLED);
            }

            AdminTokenResponse response = issueTokens(
                    admin.getId(), command.deviceId(), command.clientType());
            accountService.recordLoginSuccess(admin.getId(), command.ip());
            loginAttemptService.reset(command.username(), command.ip());
            accountService.writeLoginLog(admin.getId(), command.username(), command.deviceId(),
                    command.clientType(), command.ip(), command.userAgent(), true, "");
            return response;
        } catch (BizException exception) {
            if (exception.getErrorCode() == ErrorCode.BAD_CREDENTIALS) {
                loginAttemptService.recordFailure(command.username(), command.ip());
            }
            accountService.writeLoginLog(admin == null ? null : admin.getId(), command.username(),
                    command.deviceId(), command.clientType(), command.ip(), command.userAgent(),
                    false, exception.getMessage());
            throw exception;
        }
    }

    @Override
    public AdminTokenResponse refresh(AdminRefreshCommand command) {
        AdminSession previous = sessionService.consumeRefreshToken(
                command.refreshToken(), command.deviceId());
        accountService.getActiveById(previous.adminId());
        return issueTokens(previous.adminId(), previous.deviceId(), previous.clientType());
    }

    @Override
    public AdminMeResponse me(Long adminId) {
        SysAdminEntity admin = accountService.getActiveById(adminId);
        AdminAccess access = permissionService.getAccess(adminId);
        AdminProfile profile = new AdminProfile(
                String.valueOf(admin.getId()),
                admin.getUsername(),
                admin.getNickname(),
                admin.getAvatarUrl());
        return new AdminMeResponse(
                profile,
                access.permissions(),
                access.menus(),
                admin.getPwdResetRequired() != null && admin.getPwdResetRequired() == 1);
    }

    @Override
    public void logout(Long adminId, String sessionId) {
        sessionService.logout(adminId, sessionId);
    }

    private AdminTokenResponse issueTokens(Long adminId, String deviceId, String clientType) {
        SessionCredential credential = sessionService.createSession(adminId, deviceId, clientType);
        String accessToken = tokenService.createAccessToken(adminId, credential.sessionId());
        return new AdminTokenResponse(
                accessToken,
                credential.refreshToken(),
                "Bearer",
                tokenService.getAccessTokenExpiresIn());
    }
}

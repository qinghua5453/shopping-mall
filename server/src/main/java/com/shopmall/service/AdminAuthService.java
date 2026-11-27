package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import com.shopmall.dto.ChangePasswordRequest;
import com.shopmall.dto.LoginRequest;
import com.shopmall.dto.LoginResponse;
import com.shopmall.dto.MeResponse;
import com.shopmall.dto.PermissionContext;
import com.shopmall.entity.SysAdmin;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final SysAdminMapper adminMapper;
    private final AdminSessionService sessionService;
    private final AdminPermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;

    public LoginResponse login(LoginRequest request, String ip) {
        SysAdmin admin = adminMapper.selectByUsername(request.username());
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        adminMapper.updateLoginInfo(admin.getId(), LocalDateTime.now(), ip);
        String sessionId = sessionService.create(admin.getId());
        String accessToken = jwtUtil.createToken(admin.getId(), sessionId);
        return new LoginResponse(accessToken, "", "Bearer", authProperties.tokenTtl().toSeconds());
    }

    public MeResponse me(Long adminId) {
        SysAdmin admin = getActiveAdmin(adminId);
        PermissionContext ctx = permissionService.getContext(adminId);

        var profile = new com.shopmall.dto.AdminProfile(
                String.valueOf(admin.getId()),
                admin.getUsername(),
                admin.getNickname(),
                admin.getAvatarUrl()
        );
        boolean pwdResetRequired = admin.getPwdResetRequired() != null
                && admin.getPwdResetRequired() == 1;
        return new MeResponse(profile, ctx.permCodes(), ctx.menus(), pwdResetRequired);
    }

    public void logout(String sessionId) {
        sessionService.delete(sessionId);
    }

    @Transactional
    public void changePassword(Long adminId, ChangePasswordRequest req) {
        SysAdmin admin = getActiveAdmin(adminId);
        if (!passwordEncoder.matches(req.oldPassword(), admin.getPassword())) {
            throw new BizException(ErrorCode.OLD_PASSWORD_INVALID);
        }
        adminMapper.updatePassword(adminId, passwordEncoder.encode(req.newPassword()), 0);
        sessionService.deleteAllByAdmin(adminId);
        permissionService.invalidateAdmin(adminId);
    }

    private SysAdmin getActiveAdmin(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return admin;
    }
}

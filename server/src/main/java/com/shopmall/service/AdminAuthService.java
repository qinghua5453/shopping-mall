package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import com.shopmall.dto.AdminMenu;
import com.shopmall.dto.AdminProfile;
import com.shopmall.dto.LoginRequest;
import com.shopmall.dto.LoginResponse;
import com.shopmall.dto.MeResponse;
import com.shopmall.entity.SysAdmin;
import com.shopmall.entity.SysPermission;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.mapper.SysPermissionMapper;
import com.shopmall.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final SysAdminMapper adminMapper;
    private final SysPermissionMapper permissionMapper;
    private final AdminSessionService sessionService;
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
        List<String> roleCodes = adminMapper.selectRoleCodes(adminId);
        boolean superAdmin = roleCodes.contains("SUPER_ADMIN");
        List<SysPermission> permissions = superAdmin
                ? permissionMapper.selectAllActive()
                : permissionMapper.selectByAdminId(adminId);
        List<String> permissionCodes = superAdmin
                ? List.of("*")
                : permissions.stream().map(SysPermission::getCode).distinct().toList();

        AdminProfile profile = new AdminProfile(
                String.valueOf(admin.getId()),
                admin.getUsername(),
                admin.getNickname(),
                admin.getAvatarUrl()
        );
        boolean pwdResetRequired = admin.getPwdResetRequired() != null
                && admin.getPwdResetRequired() == 1;
        return new MeResponse(profile, permissionCodes, buildMenus(permissions), pwdResetRequired);
    }

    public void logout(String sessionId) {
        sessionService.delete(sessionId);
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

    private List<AdminMenu> buildMenus(List<SysPermission> permissions) {
        Map<Long, List<SysPermission>> byParent = permissions.stream()
                .filter(item -> item.getPermissionType() == 1 || item.getPermissionType() == 2)
                .filter(item -> item.getVisible() == 1)
                .collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildChildren(0L, byParent);
    }

    private List<AdminMenu> buildChildren(Long parentId, Map<Long, List<SysPermission>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(item -> new AdminMenu(
                        String.valueOf(item.getId()),
                        item.getName(),
                        item.getCode(),
                        item.getRoutePath(),
                        item.getIcon(),
                        buildChildren(item.getId(), byParent)))
                .toList();
    }
}

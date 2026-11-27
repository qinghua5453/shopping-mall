package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.AdminCreateRequest;
import com.shopmall.dto.AdminCreateResponse;
import com.shopmall.dto.AdminDetailResponse;
import com.shopmall.dto.AdminResponse;
import com.shopmall.dto.AdminUpdateRequest;
import com.shopmall.dto.AssignRolesRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.dto.ResetPasswordResponse;
import com.shopmall.entity.SysAdmin;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.mapper.SysRoleMapper;
import com.shopmall.util.IdGenerator;
import com.shopmall.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemAdminService {
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

    private final SysAdminMapper adminMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminSessionService sessionService;
    private final AdminPermissionService permissionService;

    public PageResult<AdminResponse> page(String keyword, Integer status, int page, int pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        long total = adminMapper.countPage(keyword, status);
        List<AdminResponse> list = adminMapper.selectPage(keyword, status, offset, pageSize)
                .stream().map(this::toListResponse).toList();
        return new PageResult<>(page, pageSize, total, list);
    }

    public AdminDetailResponse detail(Long id) {
        SysAdmin admin = getAdmin(id);
        List<String> roleIds = adminMapper.selectRoleIds(id).stream()
                .map(String::valueOf).toList();
        return new AdminDetailResponse(
                String.valueOf(admin.getId()), admin.getUsername(), admin.getNickname(), admin.getEmail(),
                admin.getStatus(), admin.getPwdResetRequired(), admin.getLastLoginAt(),
                admin.getCreatedAt(), roleIds);
    }

    @Transactional
    public AdminCreateResponse create(AdminCreateRequest req, Long currentAdminId) {
        if (adminMapper.countByUsername(req.username(), null) > 0) {
            throw new BizException(ErrorCode.USERNAME_DUPLICATE);
        }
        validateRoleIds(req.roleIds());

        SysAdmin admin = new SysAdmin();
        admin.setId(IdGenerator.nextId());
        admin.setUsername(req.username());
        String rawPassword = PasswordGenerator.generate();
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        admin.setStatus(1);
        admin.setPwdResetRequired(1);
        adminMapper.insert(admin);

        if (req.roleIds() != null && !req.roleIds().isEmpty()) {
            adminMapper.insertAdminRoles(admin.getId(), req.roleIds());
        }
        permissionService.invalidateAll();
        return new AdminCreateResponse(String.valueOf(admin.getId()), admin.getUsername(), rawPassword);
    }

    @Transactional
    public AdminDetailResponse update(Long id, AdminUpdateRequest req, Long currentAdminId) {
        SysAdmin admin = getAdmin(id);
        if (id.equals(currentAdminId) && req.status() != null && req.status() != 1) {
            throw new BizException(ErrorCode.ADMIN_SELF_PROTECT);
        }
        if (req.status() != null && req.status() != 1
                && adminMapper.selectRoleCodes(id).contains(SUPER_ADMIN_CODE)
                && adminMapper.countSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.LAST_SUPER_ADMIN);
        }
        admin.setNickname(req.nickname() == null ? admin.getNickname() : req.nickname());
        admin.setEmail(req.email() == null ? admin.getEmail() : req.email());
        admin.setStatus(req.status());
        adminMapper.update(admin);

        permissionService.invalidateAdmin(id);
        if (admin.getStatus() != 1) {
            sessionService.deleteAllByAdmin(id);
        }
        return detail(id);
    }

    @Transactional
    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new BizException(ErrorCode.ADMIN_SELF_PROTECT);
        }
        SysAdmin admin = getAdmin(id);
        if (adminMapper.selectRoleCodes(id).contains(SUPER_ADMIN_CODE)
                && adminMapper.countSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.LAST_SUPER_ADMIN);
        }
        adminMapper.deleteAdminRoles(id);
        adminMapper.deleteById(id);
        sessionService.deleteAllByAdmin(id);
        permissionService.invalidateAll();
    }

    @Transactional
    public void assignRoles(Long id, AssignRolesRequest req, Long currentAdminId) {
        getAdmin(id);
        validateRoleIds(req.roleIds());

        if (id.equals(currentAdminId)) {
            boolean currentHasSuper = adminMapper.selectRoleCodes(id).contains(SUPER_ADMIN_CODE);
            Long superRoleId = roleMapper.selectByCode(SUPER_ADMIN_CODE).getId();
            boolean newHasSuper = req.roleIds() != null && req.roleIds().contains(superRoleId);
            if (currentHasSuper && !newHasSuper) {
                throw new BizException(ErrorCode.ADMIN_SELF_PROTECT);
            }
        }
        adminMapper.deleteAdminRoles(id);
        if (req.roleIds() != null && !req.roleIds().isEmpty()) {
            adminMapper.insertAdminRoles(id, req.roleIds());
        }
        permissionService.invalidateAll();
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        getAdmin(id);
        String rawPassword = PasswordGenerator.generate();
        adminMapper.updatePassword(id, passwordEncoder.encode(rawPassword), 1);
        sessionService.deleteAllByAdmin(id);
        permissionService.invalidateAdmin(id);
        return new ResetPasswordResponse(rawPassword);
    }

    private void validateRoleIds(List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleMapper.selectById(roleId) == null) {
                throw new BizException(ErrorCode.ROLE_NOT_FOUND);
            }
        }
    }

    private SysAdmin getAdmin(Long id) {
        SysAdmin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return admin;
    }

    private AdminResponse toListResponse(SysAdmin admin) {
        return new AdminResponse(
                String.valueOf(admin.getId()), admin.getUsername(), admin.getNickname(), admin.getEmail(),
                admin.getStatus(), admin.getPwdResetRequired(), admin.getLastLoginAt(),
                admin.getCreatedAt());
    }
}

package com.shopmall.security.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminEntity;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminLoginLogEntity;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminRoleEntity;
import com.shopmall.security.infrastructure.persistence.entity.SysRoleEntity;
import com.shopmall.security.infrastructure.persistence.mapper.SysAdminLoginLogMapper;
import com.shopmall.security.infrastructure.persistence.mapper.SysAdminMapper;
import com.shopmall.security.infrastructure.persistence.mapper.SysAdminRoleMapper;
import com.shopmall.security.infrastructure.persistence.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final SysAdminMapper adminMapper;
    private final SysRoleMapper roleMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysAdminLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;

    public SysAdminEntity findByUsername(String username) {
        return adminMapper.selectOne(new LambdaQueryWrapper<SysAdminEntity>()
                .eq(SysAdminEntity::getUsername, username));
    }

    public SysAdminEntity getActiveById(Long adminId) {
        SysAdminEntity admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return admin;
    }

    @Transactional
    public SysAdminEntity createBootstrapAdmin(String username, String rawPassword, String nickname) {
        SysAdminEntity existing = findByUsername(username);
        if (existing != null) {
            return existing;
        }

        SysRoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getCode, "SUPER_ADMIN"));
        if (role == null) {
            throw new IllegalStateException("缺少内置 SUPER_ADMIN 角色，请检查 Flyway 迁移");
        }

        SysAdminEntity admin = new SysAdminEntity();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname(nickname);
        admin.setStatus(1);
        admin.setPwdResetRequired(0);
        adminMapper.insert(admin);

        SysAdminRoleEntity relation = new SysAdminRoleEntity();
        relation.setAdminId(admin.getId());
        relation.setRoleId(role.getId());
        adminRoleMapper.insert(relation);
        return admin;
    }

    public void recordLoginSuccess(Long adminId, String ip) {
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdminEntity>()
                .eq(SysAdminEntity::getId, adminId)
                .set(SysAdminEntity::getLastLoginAt, LocalDateTime.now())
                .set(SysAdminEntity::getLastLoginIp, ip));
    }

    public void writeLoginLog(Long adminId, String username, String deviceId, String clientType,
                              String ip, String userAgent, boolean success, String failReason) {
        SysAdminLoginLogEntity log = new SysAdminLoginLogEntity();
        log.setAdminId(adminId == null ? 0L : adminId);
        log.setUsername(truncate(username, 64));
        log.setDeviceId(truncate(deviceId, 64));
        log.setClientType(truncate(clientType, 20));
        log.setIp(truncate(ip, 45));
        log.setUserAgent(truncate(userAgent, 512));
        log.setSuccess(success ? 1 : 0);
        log.setFailReason(truncate(failReason, 128));
        loginLogMapper.insert(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

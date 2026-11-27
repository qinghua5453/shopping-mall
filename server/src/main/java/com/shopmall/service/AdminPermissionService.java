package com.shopmall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.annotation.Logical;
import com.shopmall.dto.AdminMenu;
import com.shopmall.dto.PermissionContext;
import com.shopmall.entity.SysPermission;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.mapper.SysPermissionMapper;
import com.shopmall.util.MenuTreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 管理员权限缓存服务：
 * - 权限码与菜单缓存到 Redis，避免每次请求查多表 JOIN；
 * - 任一 RBAC 写操作后调用 invalidateAll()，相关管理员下一次请求即生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private static final String CTX_KEY_PREFIX = "auth:rbac:ctx:";
    private static final String ADMIN_IDS_KEY = "auth:rbac:admin_ids";
    private static final String VERSION_KEY = "auth:rbac:version";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

    private final StringRedisTemplate redisTemplate;
    private final SysAdminMapper adminMapper;
    private final SysPermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    public PermissionContext getContext(Long adminId) {
        String key = ctxKey(adminId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, PermissionContext.class);
            } catch (Exception e) {
                log.warn("反序列化权限缓存失败，adminId={}", adminId, e);
            }
        }
        PermissionContext ctx = loadFromDb(adminId);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ctx), TTL);
            redisTemplate.opsForSet().add(ADMIN_IDS_KEY, String.valueOf(adminId));
        } catch (Exception e) {
            log.warn("写入权限缓存失败，adminId={}", adminId, e);
        }
        return ctx;
    }

    public boolean isSuperAdmin(Long adminId) {
        return getContext(adminId).superAdmin();
    }

    public boolean hasPermission(Long adminId, String[] codes, Logical logical) {
        if (codes == null || codes.length == 0) {
            return true;
        }
        PermissionContext ctx = getContext(adminId);
        if (ctx.superAdmin()) {
            return true;
        }
        Set<String> owned = Set.copyOf(ctx.permCodes());
        if (logical == Logical.OR) {
            for (String code : codes) {
                if (owned.contains(code)) return true;
            }
            return false;
        }
        for (String code : codes) {
            if (!owned.contains(code)) return false;
        }
        return true;
    }

    /** 清空所有管理员的权限缓存（管理员规模小可接受，实现简单不易漏）。 */
    public void invalidateAll() {
        Set<String> ids = redisTemplate.opsForSet().members(ADMIN_IDS_KEY);
        if (ids != null && !ids.isEmpty()) {
            List<String> keys = ids.stream()
                    .map(Long::valueOf)
                    .map(this::ctxKey)
                    .toList();
            redisTemplate.delete(keys);
            redisTemplate.delete(ADMIN_IDS_KEY);
        }
        redisTemplate.opsForValue().increment(VERSION_KEY);
    }

    public void invalidateAdmin(Long adminId) {
        redisTemplate.delete(ctxKey(adminId));
        redisTemplate.opsForSet().remove(ADMIN_IDS_KEY, String.valueOf(adminId));
    }

    private PermissionContext loadFromDb(Long adminId) {
        List<String> roleCodes = adminMapper.selectRoleCodes(adminId);
        boolean superAdmin = roleCodes.contains(SUPER_ADMIN_CODE);
        List<SysPermission> permissions = superAdmin
                ? permissionMapper.selectAllActive()
                : permissionMapper.selectByAdminId(adminId);
        List<String> permCodes = superAdmin
                ? List.of("*")
                : permissions.stream().map(SysPermission::getCode).distinct().toList();
        List<AdminMenu> menus = MenuTreeBuilder.build(permissions);
        return new PermissionContext(superAdmin, roleCodes, permCodes, menus);
    }

    private String ctxKey(Long adminId) {
        return CTX_KEY_PREFIX + adminId;
    }
}

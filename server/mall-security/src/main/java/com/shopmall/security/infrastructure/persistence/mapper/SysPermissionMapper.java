package com.shopmall.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopmall.security.infrastructure.persistence.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionEntity> {
    @Select("""
            SELECT DISTINCT p.*
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_admin_role ar ON ar.role_id = rp.role_id
            JOIN sys_role r ON r.id = ar.role_id AND r.status = 1
            WHERE ar.admin_id = #{adminId} AND p.status = 1
            ORDER BY p.sort, p.id
            """)
    List<SysPermissionEntity> selectByAdminId(Long adminId);
}

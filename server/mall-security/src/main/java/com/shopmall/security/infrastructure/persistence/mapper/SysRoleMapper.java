package com.shopmall.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopmall.security.infrastructure.persistence.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {
    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_admin_role ar ON ar.role_id = r.id
            WHERE ar.admin_id = #{adminId} AND r.status = 1
            """)
    List<String> selectCodesByAdminId(Long adminId);
}

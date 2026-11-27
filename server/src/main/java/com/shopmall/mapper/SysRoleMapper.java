package com.shopmall.mapper;

import com.shopmall.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper {

    List<SysRole> selectAll();

    SysRole selectById(@Param("id") Long id);

    SysRole selectByCode(@Param("code") String code);

    int insert(SysRole role);

    int update(SysRole role);

    int deleteById(@Param("id") Long id);

    List<Long> selectPermissionIds(@Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermissions(@Param("roleId") Long roleId,
                              @Param("permissionIds") List<Long> permissionIds);

    long countAdminsByRole(@Param("roleId") Long roleId);

    int deleteAdminRolesByRole(@Param("roleId") Long roleId);
}

package com.shopmall.mapper;

import com.shopmall.entity.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysPermissionMapper {
    List<SysPermission> selectAllActive();

    List<SysPermission> selectByAdminId(@Param("adminId") Long adminId);

    List<SysPermission> selectAll();

    SysPermission selectById(@Param("id") Long id);

    int update(SysPermission permission);
}

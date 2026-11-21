package com.shopmall.security.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class SysRolePermissionEntity {
    private Long roleId;
    private Long permissionId;
}

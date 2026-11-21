package com.shopmall.security.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_admin_role")
public class SysAdminRoleEntity {
    private Long adminId;
    private Long roleId;
}

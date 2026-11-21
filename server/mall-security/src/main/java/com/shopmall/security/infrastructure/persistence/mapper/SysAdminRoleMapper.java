package com.shopmall.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminRoleEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAdminRoleMapper extends BaseMapper<SysAdminRoleEntity> {
}

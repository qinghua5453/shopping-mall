package com.shopmall.mapper;

import com.shopmall.entity.SysAdmin;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysAdminMapper {
    SysAdmin selectByUsername(@Param("username") String username);

    SysAdmin selectById(@Param("id") Long id);

    List<String> selectRoleCodes(@Param("adminId") Long adminId);

    int insert(SysAdmin admin);

    int insertSuperAdminRole(@Param("adminId") Long adminId);

    int updateLoginInfo(@Param("id") Long id,
                        @Param("loginAt") LocalDateTime loginAt,
                        @Param("ip") String ip);
}

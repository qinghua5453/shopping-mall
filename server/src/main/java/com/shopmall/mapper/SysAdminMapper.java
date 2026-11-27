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

    List<SysAdmin> selectPage(@Param("keyword") String keyword,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword,
                   @Param("status") Integer status);

    int update(SysAdmin admin);

    int updatePassword(@Param("id") Long id,
                       @Param("password") String password,
                       @Param("pwdResetRequired") Integer pwdResetRequired);

    int deleteById(@Param("id") Long id);

    List<Long> selectRoleIds(@Param("adminId") Long adminId);

    int deleteAdminRoles(@Param("adminId") Long adminId);

    int insertAdminRoles(@Param("adminId") Long adminId,
                         @Param("roleIds") List<Long> roleIds);

    long countSuperAdmins();

    long countByUsername(@Param("username") String username,
                         @Param("excludeId") Long excludeId);
}

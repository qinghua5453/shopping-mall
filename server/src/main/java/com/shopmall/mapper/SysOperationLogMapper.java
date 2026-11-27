package com.shopmall.mapper;

import com.shopmall.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysOperationLogMapper {

    int insert(SysOperationLog log);

    List<SysOperationLog> selectPage(@Param("adminId") Long adminId,
                                     @Param("module") String module,
                                     @Param("success") Integer success,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long countPage(@Param("adminId") Long adminId,
                   @Param("module") String module,
                   @Param("success") Integer success,
                   @Param("start") LocalDateTime start,
                   @Param("end") LocalDateTime end);
}

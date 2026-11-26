package com.shopmall.mapper;

import com.shopmall.entity.CouponCode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponCodeMapper {
    CouponCode selectByCode(@Param("code") String code);

    List<CouponCode> selectPageByBatch(@Param("batchId") Long batchId,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    int countByBatch(@Param("batchId") Long batchId,
                     @Param("status") Integer status);

    int insert(CouponCode code);

    int batchInsert(@Param("list") List<CouponCode> codes);

    int updateStatus(@Param("id") Long id,
                     @Param("newStatus") int newStatus,
                     @Param("expected") int expectedStatus);

    int countByTemplate(@Param("templateId") Long templateId);
}

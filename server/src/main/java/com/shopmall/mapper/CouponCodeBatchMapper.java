package com.shopmall.mapper;

import com.shopmall.entity.CouponCodeBatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponCodeBatchMapper {
    CouponCodeBatch selectById(@Param("id") Long id);

    CouponCodeBatch selectByBatchNo(@Param("batchNo") String batchNo);

    List<CouponCodeBatch> selectByTemplate(@Param("templateId") Long templateId);

    List<CouponCodeBatch> selectRecent(@Param("limit") int limit);

    int insert(CouponCodeBatch batch);
}

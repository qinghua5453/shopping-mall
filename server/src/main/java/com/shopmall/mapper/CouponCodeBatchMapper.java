package com.shopmall.mapper;

import com.shopmall.entity.CouponCodeBatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponCodeBatchMapper {
    CouponCodeBatch selectById(@Param("id") Long id);

    CouponCodeBatch selectByBatchNo(@Param("batchNo") String batchNo);

    List<CouponCodeBatch> selectByTemplate(@Param("templateId") Long templateId);

    int insert(CouponCodeBatch batch);
}

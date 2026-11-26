package com.shopmall.mapper;

import com.shopmall.entity.CouponReceiveRecord;
import org.apache.ibatis.annotations.Param;

public interface CouponReceiveRecordMapper {
    CouponReceiveRecord selectByRequestNo(@Param("requestNo") String requestNo);

    int countByTemplate(@Param("templateId") Long templateId);

    int insert(CouponReceiveRecord record);

    int updateSuccess(@Param("id") Long id,
                      @Param("couponId") Long couponId);
}

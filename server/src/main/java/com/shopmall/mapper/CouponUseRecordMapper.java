package com.shopmall.mapper;

import com.shopmall.entity.CouponUseRecord;
import org.apache.ibatis.annotations.Param;

public interface CouponUseRecordMapper {
    CouponUseRecord findUnique(@Param("couponId") Long couponId,
                               @Param("action") int action,
                               @Param("orderNo") String orderNo);

    int insert(CouponUseRecord record);
}

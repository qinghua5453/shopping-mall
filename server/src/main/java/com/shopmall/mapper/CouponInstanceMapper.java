package com.shopmall.mapper;

import com.shopmall.entity.CouponInstance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponInstanceMapper {
    CouponInstance selectById(@Param("id") Long id);

    CouponInstance selectByCouponNo(@Param("couponNo") String couponNo);

    List<CouponInstance> selectPage(@Param("memberId") Long memberId,
                                    @Param("templateId") Long templateId,
                                    @Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    int countPage(@Param("memberId") Long memberId,
                  @Param("templateId") Long templateId,
                  @Param("status") Integer status);

    int insert(CouponInstance instance);

    int updateStatus(@Param("id") Long id,
                     @Param("status") int status,
                     @Param("currentVersion") int currentVersion);

    int countByMemberAndTemplate(@Param("memberId") Long memberId,
                                 @Param("templateId") Long templateId);
}

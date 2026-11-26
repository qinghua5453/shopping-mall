package com.shopmall.mapper;

import com.shopmall.entity.CouponTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponTemplateMapper {
    CouponTemplate selectById(@Param("id") Long id);

    CouponTemplate selectByTemplateNo(@Param("templateNo") String templateNo);

    List<CouponTemplate> selectPage(@Param("status") Integer status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    int countPage(@Param("status") Integer status,
                  @Param("keyword") String keyword);

    int insert(CouponTemplate template);

    int update(CouponTemplate template);

    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id,
                     @Param("status") int status,
                     @Param("currentVersion") int currentVersion);

    int incrementIssued(@Param("id") Long id,
                        @Param("currentVersion") int currentVersion);

    int countReceiveRecords(@Param("templateId") Long templateId);

    int countCodeByTemplate(@Param("templateId") Long templateId);
}

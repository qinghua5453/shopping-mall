package com.shopmall.mapper;

import com.shopmall.entity.CouponStackRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponStackRuleMapper {
    List<CouponStackRule> selectAll();

    CouponStackRule selectByGroups(@Param("groupA") String groupA,
                                   @Param("groupB") String groupB);

    int insert(CouponStackRule rule);

    int update(CouponStackRule rule);

    int deleteByGroups(@Param("groupA") String groupA,
                       @Param("groupB") String groupB);
}

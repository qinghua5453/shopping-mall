package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.CouponStackRuleUpsertRequest;
import com.shopmall.entity.CouponStackRule;
import com.shopmall.mapper.CouponStackRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponStackRuleService {
    private final CouponStackRuleMapper ruleMapper;

    public List<CouponStackRule> list() {
        return ruleMapper.selectAll();
    }

    @Transactional
    public CouponStackRule upsert(CouponStackRuleUpsertRequest req) {
        String a = req.groupA().trim();
        String b = req.groupB().trim();
        int cmp = a.compareTo(b);
        String groupA = cmp < 0 ? a : b;
        String groupB = cmp < 0 ? b : a;

        CouponStackRule existing = ruleMapper.selectByGroups(groupA, groupB);
        int maxCount = req.maxStackCount() == null ? 3 : req.maxStackCount();
        int status = req.status() == null ? 1 : req.status();

        if (existing == null) {
            CouponStackRule rule = new CouponStackRule();
            rule.setGroupA(groupA);
            rule.setGroupB(groupB);
            rule.setAllowStack(req.allowStack());
            rule.setMaxStackCount(maxCount);
            rule.setStatus(status);
            ruleMapper.insert(rule);
            return rule;
        } else {
            existing.setAllowStack(req.allowStack());
            existing.setMaxStackCount(maxCount);
            existing.setStatus(status);
            ruleMapper.update(existing);
            return ruleMapper.selectByGroups(groupA, groupB);
        }
    }

    @Transactional
    public void delete(String groupA, String groupB) {
        ruleMapper.deleteByGroups(groupA.trim(), groupB.trim());
    }
}

package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.CouponGrantRequest;
import com.shopmall.dto.CouponInstanceFreezeRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.CouponInstance;
import com.shopmall.entity.CouponTemplate;
import com.shopmall.entity.CouponUseRecord;
import com.shopmall.mapper.CouponInstanceMapper;
import com.shopmall.mapper.CouponTemplateMapper;
import com.shopmall.mapper.CouponUseRecordMapper;
import com.shopmall.util.CouponNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponInstanceService {
    private static final int INSTANCE_UNUSED = 1;
    private static final int INSTANCE_FROZEN = 5;

    private final CouponInstanceMapper instanceMapper;
    private final CouponTemplateMapper templateMapper;
    private final CouponUseRecordMapper useRecordMapper;

    public PageResult<CouponInstance> page(Long memberId, Long templateId, Integer status, int page, int pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        long total = instanceMapper.countPage(memberId, templateId, status);
        var list = instanceMapper.selectPage(memberId, templateId, status, offset, pageSize);
        return new PageResult<>(page, pageSize, total, list);
    }

    @Transactional
    public CouponInstance freeze(Long id, CouponInstanceFreezeRequest req, Long adminId) {
        CouponInstance inst = instanceMapper.selectById(id);
        if (inst == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (inst.getStatus() != INSTANCE_UNUSED) {
            throw new BizException(ErrorCode.COUPON_STATUS_INVALID,
                    "只有未使用的券可以冻结（当前状态：" + inst.getStatus() + "）");
        }
        int affected = instanceMapper.updateStatus(id, INSTANCE_FROZEN, inst.getVersion());
        if (affected == 0) throw new BizException(ErrorCode.COUPON_STATUS_INVALID, "并发冲突，请重试");

        CouponUseRecord rec = buildUseRecord(inst, 5, INSTANCE_UNUSED, INSTANCE_FROZEN, adminId, req.reason());
        useRecordMapper.insert(rec);
        return instanceMapper.selectById(id);
    }

    @Transactional
    public CouponInstance unfreeze(Long id, Long adminId) {
        CouponInstance inst = instanceMapper.selectById(id);
        if (inst == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (inst.getStatus() != INSTANCE_FROZEN) {
            throw new BizException(ErrorCode.COUPON_STATUS_INVALID, "只有冻结状态可以解冻");
        }
        int newStatus = inst.getValidEnd() != null && inst.getValidEnd().isBefore(LocalDateTime.now()) ? 4 : INSTANCE_UNUSED;
        int affected = instanceMapper.updateStatus(id, newStatus, inst.getVersion());
        if (affected == 0) throw new BizException(ErrorCode.COUPON_STATUS_INVALID, "并发冲突，请重试");

        CouponUseRecord rec = buildUseRecord(inst, 7, INSTANCE_FROZEN, newStatus, adminId, "后台解冻");
        useRecordMapper.insert(rec);
        return instanceMapper.selectById(id);
    }

    @Transactional
    public void grant(CouponGrantRequest req, Long adminId) {
        CouponTemplate t = templateMapper.selectById(req.templateId());
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (t.getStatus() != 1) throw new BizException(ErrorCode.TEMPLATE_STATUS_INVALID,
                "只有已发布模板可以定向发券");

        LocalDateTime validStart;
        LocalDateTime validEnd;
        if (t.getValidityType() == 1) {
            validStart = t.getValidStart();
            validEnd = t.getValidEnd();
        } else {
            validStart = LocalDateTime.now();
            validEnd = validStart.plusDays(t.getValidDays());
        }

        int successCount = 0;
        for (Long memberId : req.memberIds()) {
            try {
                CouponInstance inst = new CouponInstance();
                inst.setCouponNo(CouponNoGenerator.couponNo());
                inst.setTemplateId(t.getId());
                inst.setMemberId(memberId);
                inst.setStatus(INSTANCE_UNUSED);
                inst.setSource("ADMIN");
                inst.setReceiveChannel("ADMIN");
                inst.setReceiveTime(LocalDateTime.now());
                inst.setValidStart(validStart);
                inst.setValidEnd(validEnd);
                inst.setDiscountAmount(0L);
                inst.setVersion(0);
                instanceMapper.insert(inst);

                if (t.getTotalQuantity() != null && t.getTotalQuantity() > 0) {
                    templateMapper.incrementIssued(t.getId(), t.getVersion());
                }
                successCount++;
            } catch (Exception ex) {
                log.warn("定向发券跳过，templateId={}, memberId={}, error={}", req.templateId(), memberId, ex.getMessage());
            }
        }
        log.info("定向发券完成，templateId={}, total={}, success={}", req.templateId(), req.memberIds().size(), successCount);
    }

    private CouponUseRecord buildUseRecord(CouponInstance inst, int action, int before, int after, Long adminId, String remark) {
        CouponUseRecord rec = new CouponUseRecord();
        rec.setCouponId(inst.getId());
        rec.setCouponNo(inst.getCouponNo());
        rec.setMemberId(inst.getMemberId());
        rec.setTemplateId(inst.getTemplateId());
        rec.setAction(action);
        rec.setBeforeStatus(before);
        rec.setAfterStatus(after);
        rec.setDiscountAmount(inst.getDiscountAmount() == null ? 0L : inst.getDiscountAmount());
        rec.setOperatorType("ADMIN");
        rec.setOperatorId(adminId == null ? 0L : adminId);
        rec.setRemark(remark == null ? "" : remark);
        rec.setTraceId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        return rec;
    }
}

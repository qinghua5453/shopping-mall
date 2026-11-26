package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.CouponTemplateCreateRequest;
import com.shopmall.dto.CouponTemplateResponse;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.CouponTemplate;
import com.shopmall.mapper.CouponTemplateMapper;
import com.shopmall.util.CouponNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class CouponTemplateService {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    private final CouponTemplateMapper templateMapper;

    public PageResult<CouponTemplateResponse> page(Integer status, String keyword, int page, int pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        long total = templateMapper.countPage(status, keyword);
        var list = templateMapper.selectPage(status, keyword, offset, pageSize).stream()
                .map(CouponTemplateResponse::from).toList();
        return new PageResult<>(page, pageSize, total, list);
    }

    public CouponTemplateResponse detail(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        return CouponTemplateResponse.from(t);
    }

    @Transactional
    public CouponTemplateResponse create(CouponTemplateCreateRequest req, Long adminId) {
        CouponTemplate t = new CouponTemplate();
        t.setTemplateNo(CouponNoGenerator.templateNo());
        t.setName(req.name());
        t.setCouponType(req.couponType());
        t.setScopeType(req.scopeType());
        t.setScopeValues(normalizeJson(req.scopeValues()));
        t.setThresholdAmount(req.thresholdAmount());
        t.setDiscountAmount(req.discountAmount());
        t.setDiscountRate(req.discountRate());
        t.setMaxDiscountAmount(req.maxDiscountAmount() == null ? 0L : req.maxDiscountAmount());
        t.setTotalQuantity(req.totalQuantity());
        t.setIssuedQuantity(0);
        t.setPerUserLimit(req.perUserLimit());
        t.setValidityType(req.validityType());
        t.setValidStart(parseDateTime(req.validStart()));
        t.setValidEnd(parseDateTime(req.validEnd()));
        t.setValidDays(req.validDays() == null ? 0 : req.validDays());
        t.setReceiveStart(parseDateTime(req.receiveStart()));
        t.setReceiveEnd(parseDateTime(req.receiveEnd()));
        t.setStackGroup(req.stackGroup());
        t.setStackable(req.stackable() == null ? 1 : req.stackable());
        t.setRefundPolicy(req.refundPolicy() == null ? 1 : req.refundPolicy());
        t.setReceiveChannels(req.receiveChannels() == null ? "ALL" : req.receiveChannels());
        t.setSortWeight(req.sortWeight() == null ? 0 : req.sortWeight());
        t.setStatus(STATUS_DRAFT);
        t.setRemark(req.remark() == null ? "" : req.remark());
        t.setVersion(0);
        t.setCreatedBy(adminId == null ? 0L : adminId);
        t.setUpdatedBy(adminId == null ? 0L : adminId);
        validateForCreate(t);
        templateMapper.insert(t);
        return CouponTemplateResponse.from(t);
    }

    @Transactional
    public CouponTemplateResponse update(Long id, CouponTemplateCreateRequest req, Long adminId) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (t.getStatus() == STATUS_PUBLISHED) {
            throw new BizException(ErrorCode.TEMPLATE_PUBLISHED_FIELDS_LOCKED,
                    "已发布模板的关键字段不可修改，请先下线");
        }
        CouponTemplate update = new CouponTemplate();
        update.setId(id);
        update.setVersion(t.getVersion());
        update.setName(req.name());
        update.setCouponType(req.couponType());
        update.setScopeType(req.scopeType());
        update.setScopeValues(normalizeJson(req.scopeValues()));
        update.setThresholdAmount(req.thresholdAmount());
        update.setDiscountAmount(req.discountAmount());
        update.setDiscountRate(req.discountRate());
        update.setMaxDiscountAmount(req.maxDiscountAmount() == null ? 0L : req.maxDiscountAmount());
        update.setTotalQuantity(req.totalQuantity());
        update.setPerUserLimit(req.perUserLimit());
        update.setValidityType(req.validityType());
        update.setValidStart(parseDateTime(req.validStart()));
        update.setValidEnd(parseDateTime(req.validEnd()));
        update.setValidDays(req.validDays() == null ? 0 : req.validDays());
        update.setReceiveStart(parseDateTime(req.receiveStart()));
        update.setReceiveEnd(parseDateTime(req.receiveEnd()));
        update.setStackGroup(req.stackGroup());
        update.setStackable(req.stackable() == null ? 1 : req.stackable());
        update.setRefundPolicy(req.refundPolicy() == null ? 1 : req.refundPolicy());
        update.setReceiveChannels(req.receiveChannels() == null ? "ALL" : req.receiveChannels());
        update.setSortWeight(req.sortWeight() == null ? 0 : req.sortWeight());
        update.setRemark(req.remark() == null ? "" : req.remark());
        update.setUpdatedBy(adminId == null ? 0L : adminId);
        validateForCreate(update);
        int affected = templateMapper.update(update);
        if (affected == 0) {
            throw new BizException(ErrorCode.TEMPLATE_PUBLISHED_FIELDS_LOCKED,
                    "更新失败，模板可能已发布或版本冲突");
        }
        return CouponTemplateResponse.from(templateMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (t.getStatus() != STATUS_DRAFT) {
            throw new BizException(ErrorCode.TEMPLATE_STATUS_INVALID,
                    "只有草稿状态可以删除");
        }
        int receiveCount = templateMapper.countReceiveRecords(id);
        int codeCount = templateMapper.countCodeByTemplate(id);
        if (receiveCount > 0 || codeCount > 0) {
            throw new BizException(ErrorCode.TEMPLATE_HAS_RECORDS,
                    "模板已有领取记录或券码，不可删除");
        }
        templateMapper.deleteById(id);
    }

    @Transactional
    public CouponTemplateResponse publish(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (t.getStatus() == STATUS_PUBLISHED) {
            throw new BizException(ErrorCode.TEMPLATE_STATUS_INVALID, "模板已发布");
        }
        if (t.getReceiveStart() != null && t.getReceiveEnd() != null && t.getReceiveStart().isAfter(t.getReceiveEnd())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "领取开始时间不能晚于结束时间");
        }
        if (t.getValidityType() == 1 && (t.getValidStart() == null || t.getValidEnd() == null)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "固定有效期类型需要填写 validStart 和 validEnd");
        }
        if (t.getValidityType() == 2 && (t.getValidDays() == null || t.getValidDays() <= 0)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "领取后 N 天类型需要 validDays > 0");
        }
        int affected = templateMapper.updateStatus(id, STATUS_PUBLISHED, t.getVersion());
        if (affected == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "发布失败，请重试");
        return CouponTemplateResponse.from(templateMapper.selectById(id));
    }

    @Transactional
    public CouponTemplateResponse offline(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (t.getStatus() != STATUS_PUBLISHED) {
            throw new BizException(ErrorCode.TEMPLATE_STATUS_INVALID, "只有已发布可以下线");
        }
        int affected = templateMapper.updateStatus(id, STATUS_OFFLINE, t.getVersion());
        if (affected == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "下线失败，请重试");
        return CouponTemplateResponse.from(templateMapper.selectById(id));
    }

    private void validateForCreate(CouponTemplate t) {
        if (t.getCouponType() == null || t.getCouponType() < 1 || t.getCouponType() > 3) {
            throw new BizException(ErrorCode.PARAM_ERROR, "couponType 必须是 1/2/3");
        }
        if (t.getThresholdAmount() != null && t.getThresholdAmount() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "门槛金额不能为负");
        }
        if (t.getDiscountAmount() != null && t.getDiscountAmount() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "优惠金额不能为负");
        }
        if (t.getCouponType() == 1 && t.getDiscountAmount() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "满减券需要 discountAmount");
        }
        if (t.getCouponType() == 2 && (t.getDiscountRate() == null || t.getDiscountRate() <= 0 || t.getDiscountRate() > 10)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "折扣券 discountRate 范围 (0, 10]");
        }
        if (t.getTotalQuantity() != null && t.getTotalQuantity() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "总量不能为负");
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "时间格式错误：" + s + "，期望 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String normalizeJson(String s) {
        if (s == null || s.isBlank()) return "{}";
        return s;
    }
}

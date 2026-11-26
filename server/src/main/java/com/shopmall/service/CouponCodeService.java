package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.CouponCodeBatchResponse;
import com.shopmall.dto.CouponCodeGenerateRequest;
import com.shopmall.dto.PageResult;
import com.shopmall.entity.CouponCode;
import com.shopmall.entity.CouponCodeBatch;
import com.shopmall.entity.CouponTemplate;
import com.shopmall.mapper.CouponCodeBatchMapper;
import com.shopmall.mapper.CouponCodeMapper;
import com.shopmall.mapper.CouponTemplateMapper;
import com.shopmall.util.CouponNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponCodeService {
    private static final int CODE_UNUSED = 1;
    private static final int CODE_USED = 2;
    private static final int CODE_VOID = 3;
    private static final int BATCH_STATUS_OK = 1;

    private final CouponCodeMapper codeMapper;
    private final CouponCodeBatchMapper batchMapper;
    private final CouponTemplateMapper templateMapper;

    @Transactional
    public CouponCodeBatchResponse generate(Long templateId, CouponCodeGenerateRequest req, Long adminId) {
        CouponTemplate t = templateMapper.selectById(templateId);
        if (t == null) throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        if (req.count() <= 0 || req.count() > 5000) {
            throw new BizException(ErrorCode.BATCH_SIZE_TOO_LARGE);
        }

        CouponCodeBatch batch = new CouponCodeBatch();
        batch.setBatchNo(CouponNoGenerator.batchNo());
        batch.setTemplateId(templateId);
        batch.setTotal(req.count());
        batch.setStatus(BATCH_STATUS_OK);
        batch.setCreatedBy(adminId == null ? 0L : adminId);
        batchMapper.insert(batch);

        List<CouponCode> codes = new ArrayList<>(req.count());
        for (int i = 0; i < req.count(); i++) {
            CouponCode code = new CouponCode();
            code.setCode(CouponNoGenerator.couponCode());
            code.setBatchId(batch.getId());
            code.setTemplateId(templateId);
            code.setStatus(CODE_UNUSED);
            codes.add(code);
        }
        codeMapper.batchInsert(codes);

        return CouponCodeBatchResponse.from(batch);
    }

    public List<CouponCodeBatch> listBatches(int limit) {
        if (limit <= 0 || limit > 200) limit = 50;
        return batchMapper.selectRecent(limit);
    }

    public PageResult<CouponCode> pageByBatch(Long batchId, Integer status, int page, int pageSize) {
        CouponCodeBatch b = batchMapper.selectById(batchId);
        if (b == null) throw new BizException(ErrorCode.BATCH_NOT_FOUND);
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        long total = codeMapper.countByBatch(batchId, status);
        var list = codeMapper.selectPageByBatch(batchId, status, offset, pageSize);
        return new PageResult<>(page, pageSize, total, list);
    }

    @Transactional
    public void voidCode(String codeStr) {
        CouponCode code = codeMapper.selectByCode(codeStr);
        if (code == null) throw new BizException(ErrorCode.COUPON_CODE_INVALID);
        if (code.getStatus() == CODE_USED) throw new BizException(ErrorCode.COUPON_CODE_USED);
        if (code.getStatus() == CODE_VOID) throw new BizException(ErrorCode.COUPON_CODE_VOIDED);
        int affected = codeMapper.updateStatus(code.getId(), CODE_VOID, CODE_UNUSED);
        if (affected == 0) throw new BizException(ErrorCode.COUPON_STATUS_INVALID, "券码已被占用");
    }
}

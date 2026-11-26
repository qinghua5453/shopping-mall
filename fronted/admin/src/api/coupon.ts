import { api } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  CouponCode,
  CouponCodeBatch,
  CouponCodeGenerateForm,
  CouponGrantForm,
  CouponInstance,
  CouponInstanceFreezeForm,
  CouponStackRule,
  CouponStackRuleForm,
  CouponTemplate,
  CouponTemplateForm,
} from '@/types/coupon'

const BASE = '/api/v1/admin/coupons'

export const couponApi = {
  // ===== 模板 =====
  pageTemplates(params: {
    status?: number
    keyword?: string
    page?: number
    pageSize?: number
  }) {
    return api.get<PageResult<CouponTemplate>>(`${BASE}/templates`, { params })
  },
  getTemplate(id: number) {
    return api.get<CouponTemplate>(`${BASE}/templates/${id}`)
  },
  createTemplate(body: CouponTemplateForm) {
    return api.post<CouponTemplate>(`${BASE}/templates`, body)
  },
  updateTemplate(id: number, body: CouponTemplateForm) {
    return api.put<CouponTemplate>(`${BASE}/templates/${id}`, body)
  },
  deleteTemplate(id: number) {
    return api.delete<void>(`${BASE}/templates/${id}`)
  },
  publishTemplate(id: number) {
    return api.post<CouponTemplate>(`${BASE}/templates/${id}/publish`)
  },
  offlineTemplate(id: number) {
    return api.post<CouponTemplate>(`${BASE}/templates/${id}/offline`)
  },

  // ===== 券码 =====
  generateCodes(templateId: number, body: CouponCodeGenerateForm) {
    return api.post<CouponCodeBatch>(`${BASE}/codes/generate`, body, {
      params: { templateId },
    })
  },
  pageCodes(params: {
    batchId: number
    status?: number
    page?: number
    pageSize?: number
  }) {
    return api.get<PageResult<CouponCode>>(`${BASE}/codes`, { params })
  },
  voidCode(code: string) {
    return api.post<void>(`${BASE}/codes/void`, null, {
      params: { code },
    })
  },

  // ===== 用户券 =====
  pageInstances(params: {
    memberId?: number
    templateId?: number
    status?: number
    page?: number
    pageSize?: number
  }) {
    return api.get<PageResult<CouponInstance>>(`${BASE}/instances`, { params })
  },
  freezeInstance(id: number, body: CouponInstanceFreezeForm) {
    return api.post<CouponInstance>(`${BASE}/instances/${id}/freeze`, body)
  },
  unfreezeInstance(id: number) {
    return api.post<CouponInstance>(`${BASE}/instances/${id}/unfreeze`)
  },
  grant(body: CouponGrantForm) {
    return api.post<void>(`${BASE}/instances/grant`, body)
  },

  // ===== 叠加规则 =====
  listStackRules() {
    return api.get<CouponStackRule[]>(`${BASE}/stack-rules`)
  },
  upsertStackRule(body: CouponStackRuleForm) {
    return api.post<CouponStackRule>(`${BASE}/stack-rules`, body)
  },
  deleteStackRule(groupA: string, groupB: string) {
    return api.delete<void>(`${BASE}/stack-rules`, { params: { groupA, groupB } })
  },
}

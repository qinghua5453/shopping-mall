// ===== 枚举常量 =====

/** 模板状态 */
export const TEMPLATE_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  OFFLINE: 2,
} as const

export type TemplateStatus = (typeof TEMPLATE_STATUS)[keyof typeof TEMPLATE_STATUS]

export const TEMPLATE_STATUS_LABEL: Record<TemplateStatus, string> = {
  0: '草稿',
  1: '已发布',
  2: '已下线',
}

export const TEMPLATE_STATUS_TYPE: Record<TemplateStatus, 'info' | 'success' | 'warning'> = {
  0: 'info',
  1: 'success',
  2: 'warning',
}

/** 券类型 */
export const COUPON_TYPE = {
  FULL_REDUCTION: 1,
  DISCOUNT: 2,
  NO_THRESHOLD: 3,
} as const

export const COUPON_TYPE_LABEL: Record<number, string> = {
  1: '满减券',
  2: '折扣券',
  3: '无门槛券',
}

/** 适用范围 */
export const SCOPE_TYPE_LABEL: Record<number, string> = {
  1: '全场',
  2: '类目',
  3: 'SPU',
  4: 'SKU',
}

/** 有效期类型 */
export const VALIDITY_TYPE_LABEL: Record<number, string> = {
  1: '固定区间',
  2: '领取后 N 天',
}

/** 券码状态 */
export const CODE_STATUS_LABEL: Record<number, string> = {
  1: '未使用',
  2: '已使用',
  3: '已作废',
}

export const CODE_STATUS_TYPE: Record<number, 'info' | 'success' | 'danger'> = {
  1: 'info',
  2: 'success',
  3: 'danger',
}

/** 券实例状态 */
export const INSTANCE_STATUS_LABEL: Record<number, string> = {
  1: '未使用',
  2: '锁定中',
  3: '已使用',
  4: '已过期',
  5: '已冻结',
}

export const INSTANCE_STATUS_TYPE: Record<number, string> = {
  1: 'success',
  2: 'warning',
  3: 'info',
  4: 'info',
  5: 'danger',
}

// ===== 后端实体 =====

export interface CouponTemplate {
  id: number
  templateNo: string
  name: string
  couponType: number
  scopeType: number
  scopeValues: string
  thresholdAmount: number
  discountAmount: number
  discountRate: number | null
  maxDiscountAmount: number | null
  totalQuantity: number
  issuedQuantity: number
  perUserLimit: number
  validityType: number
  validStart: string | null
  validEnd: string | null
  validDays: number | null
  receiveStart: string | null
  receiveEnd: string | null
  stackGroup: string
  stackable: number
  refundPolicy: number
  receiveChannels: string
  sortWeight: number
  status: number
  remark: string
  createdAt: string
  updatedAt: string
}

export interface CouponCode {
  id: number
  code: string
  batchId: number
  templateId: number
  status: number
  memberId: number | null
  usedTime: string | null
  createdAt: string
  updatedAt: string
}

export interface CouponCodeBatch {
  id: number
  batchNo: string
  templateId: number
  total: number
  status: number
  createdAt: string
}

export interface CouponInstance {
  id: number
  couponNo: string
  templateId: number
  memberId: number
  status: number
  source: string
  receiveChannel: string
  receiveTime: string
  validStart: string
  validEnd: string
  usedTime: string | null
  orderNo: string
  lockOrderNo: string
  discountAmount: number
  version: number
  createdAt: string
  updatedAt: string
}

export interface CouponStackRule {
  id: number
  groupA: string
  groupB: string
  allowStack: number
  maxStackCount: number
  status: number
  createdAt: string
  updatedAt: string
}

// ===== 前端表单 DTO =====

/** 新建/修改模板（共用，与后端 CouponTemplateCreateRequest 对齐） */
export interface CouponTemplateForm {
  name: string
  couponType: number
  scopeType: number
  scopeValues?: string
  thresholdAmount: number
  discountAmount: number
  discountRate?: number
  maxDiscountAmount?: number
  totalQuantity: number
  perUserLimit: number
  validityType: number
  validStart?: string
  validEnd?: string
  validDays?: number
  receiveStart?: string
  receiveEnd?: string
  stackGroup: string
  stackable?: number
  refundPolicy?: number
  receiveChannels?: string
  sortWeight?: number
  remark?: string
}

/** 叠加规则 UPSERT */
export interface CouponStackRuleForm {
  groupA: string
  groupB: string
  allowStack: number
  maxStackCount: number
  status?: number
}

/** 冻结原因 */
export interface CouponInstanceFreezeForm {
  reason: string
}

/** 定向发券 */
export interface CouponGrantForm {
  templateId: number
  memberIds: number[]
  reason?: string
}

/** 生成券码 */
export interface CouponCodeGenerateForm {
  count: number
}

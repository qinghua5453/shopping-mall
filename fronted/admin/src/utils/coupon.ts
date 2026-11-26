import {
  COUPON_TYPE_LABEL,
  SCOPE_TYPE_LABEL,
  VALIDITY_TYPE_LABEL,
} from '@/types/coupon'

/** 分 → 元，保留两位小数 */
export function fenToYuan(fen: number | null | undefined): string {
  if (fen === null || fen === undefined) return '-'
  return (fen / 100).toFixed(2)
}

/** 折扣率 → "x.x 折"，discountRate=8.5 → "8.5 折" */
export function formatDiscountRate(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return '-'
  return `${rate.toString().replace(/\.0$/, '')} 折`
}

/** 把 template 的优惠规则格式化成可读描述："满 100.00 减 20.00" / "全场 8.5 折" */
export function formatCouponRule(
  couponType: number,
  thresholdAmount: number,
  discountAmount: number,
  discountRate: number | null | undefined,
  maxDiscountAmount: number | null | undefined,
): string {
  if (couponType === 2) {
    // 折扣券
    const base = `${formatDiscountRate(discountRate)}`
    if (maxDiscountAmount && maxDiscountAmount > 0) {
      return `${base}（封顶 ¥${fenToYuan(maxDiscountAmount)}）`
    }
    return base
  }
  // 满减 / 无门槛
  const off = `减 ${fenToYuan(discountAmount)}`
  if (thresholdAmount > 0) {
    return `满 ${fenToYuan(thresholdAmount)} ${off}`
  }
  return `无门槛 ${off}`
}

/** validityType + 时间 → 可读有效期 */
export function formatValidity(
  validityType: number,
  validDays: number | null | undefined,
  validStart: string | null | undefined,
  validEnd: string | null | undefined,
): string {
  if (validityType === 2 && validDays) return `领取后 ${validDays} 天`
  if (validityType === 1) {
    const s = validStart ? stripSeconds(validStart) : '?'
    const e = validEnd ? stripSeconds(validEnd) : '?'
    return `${s} ~ ${e}`
  }
  return '-'
}

function stripSeconds(s: string): string {
  // "2026-06-01 00:00:00" → "2026-06-01 00:00"；ISO "2026-06-01T00:00:00" → 同上
  return s.replace('T', ' ').replace(/:\d{2}$/, '')
}

export function couponTypeLabel(t: number) {
  return COUPON_TYPE_LABEL[t] ?? `未知(${t})`
}
export function scopeTypeLabel(t: number) {
  return SCOPE_TYPE_LABEL[t] ?? `未知(${t})`
}
export function validityTypeLabel(t: number) {
  return VALIDITY_TYPE_LABEL[t] ?? `未知(${t})`
}

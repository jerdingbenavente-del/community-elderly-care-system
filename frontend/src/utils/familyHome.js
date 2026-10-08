/**
 * 家属端通用展示工具
 */
export function unwrap(res) {
  if (!res || res.code !== 200) {
    throw new Error(res?.message || '获取数据失败')
  }
  return res.data
}

export function calcAge(birthDate) {
  if (!birthDate) return null
  const d = new Date(birthDate)
  if (Number.isNaN(d.getTime())) return null
  const now = new Date()
  let age = now.getFullYear() - d.getFullYear()
  const m = now.getMonth() - d.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age -= 1
  return age >= 0 ? age : null
}

export function genderText(gender) {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '未知'
}

export const ORDER_STATUS = {
  PENDING: { label: '待确认', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  IN_SERVICE: { label: '服务中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

export function orderStatusMeta(status) {
  return ORDER_STATUS[status] || { label: status || '-', type: 'info' }
}

export const PAYMENT_STATUS = {
  UNPAID: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'success' },
}

export function paymentStatusMeta(status) {
  return PAYMENT_STATUS[status] || { label: status || '待支付', type: 'info' }
}

/** 服务类型展示（后端 serviceType） */
export const SERVICE_TYPE_LABEL = {
  DAILY: '生活照料',
  REHAB: '康复服务',
  HEALTH: '健康护理',
  COMPANION: '陪伴服务',
}

export function serviceTypeLabel(type) {
  if (!type) return '照护服务'
  return SERVICE_TYPE_LABEL[type] || type
}

export function formatPrice(price) {
  if (price == null || price === '') return '-'
  const n = Number(price)
  if (Number.isNaN(n)) return String(price)
  return `¥ ${n.toFixed(2)}`
}

export const WARNING_STATUS = {
  UNHANDLED: { label: '待处理', type: 'danger' },
  HANDLED: { label: '已处理', type: 'success' },
}

export function warningStatusMeta(status) {
  return WARNING_STATUS[status] || { label: status || '-', type: 'info' }
}

/** 后端 warning_level：WARNING / CRITICAL */
export const WARNING_LEVEL = {
  WARNING: { label: '一般预警', tone: 'mid' },
  CRITICAL: { label: '高风险', tone: 'high' },
}

export function warningLevelMeta(level) {
  return WARNING_LEVEL[level] || { label: level || '预警', tone: 'low' }
}

export const INDICATOR_LABEL = {
  SYSTOLIC_PRESSURE: '收缩压',
  DIASTOLIC_PRESSURE: '舒张压',
  BLOOD_GLUCOSE: '血糖',
  TEMPERATURE: '体温',
  HEART_RATE: '心率',
}

export function indicatorLabel(code) {
  return INDICATOR_LABEL[code] || code || '-'
}

export function directionLabel(direction) {
  if (direction === 'HIGH') return '偏高'
  if (direction === 'LOW') return '偏低'
  return direction || ''
}

export function formatDateTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

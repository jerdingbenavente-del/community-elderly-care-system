/**
 * 前端敏感信息脱敏（不依赖后端是否已脱敏）
 */

/** 身份证：前 6 + **** + 后 4；已含 * 则保持原样 */
export function maskIdCard(idCard) {
  if (idCard == null || idCard === '') return '-'
  const value = String(idCard).trim()
  if (!value) return '-'
  if (value.includes('*')) return value
  if (value.length < 10) return '****'
  return `${value.slice(0, 6)}****${value.slice(-4)}`
}

/** 手机号：前 3 + **** + 后 4 */
export function maskPhone(phone) {
  if (phone == null || phone === '') return '-'
  const value = String(phone).trim()
  if (!value) return '-'
  if (value.includes('*')) return value
  if (value.length < 7) return '****'
  return `${value.slice(0, 3)}****${value.slice(-4)}`
}

/** 档案状态文案（ElderVO.status） */
export function elderStatusText(status) {
  if (status === 1) return '档案正常'
  if (status === 0) return '已停用'
  return '未知状态'
}

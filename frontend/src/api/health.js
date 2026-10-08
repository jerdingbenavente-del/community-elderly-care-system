import request from './request'

/**
 * 家属端健康记录列表
 * GET /api/family/elders/{elderId}/health-records
 * 查询：measuredFrom, measuredTo, page, size（后端对家属仅 LIMIT size，无 offset）
 */
export function listFamilyHealthRecords(elderId, params = {}) {
  return request.get(`/family/elders/${elderId}/health-records`, { params })
}

/** GET /api/family/health-records/{id} */
export function getFamilyHealthRecord(id) {
  return request.get(`/family/health-records/${id}`)
}

/**
 * 家属端健康预警列表（含 UNHANDLED / HANDLED）
 * GET /api/family/elders/{elderId}/health-warnings
 */
export function listFamilyHealthWarnings(elderId) {
  return request.get(`/family/elders/${elderId}/health-warnings`)
}

/** GET /api/family/health-warnings/{id} */
export function getFamilyHealthWarning(id) {
  return request.get(`/family/health-warnings/${id}`)
}

/** 别名：与 F4 规范命名对齐 */
export function getHealthRecords(elderId, params = {}) {
  return listFamilyHealthRecords(elderId, params)
}

export function getHealthRecordDetail(id) {
  return getFamilyHealthRecord(id)
}

export function getHealthAlerts(elderId) {
  return listFamilyHealthWarnings(elderId)
}

export function getHealthAlertDetail(id) {
  return getFamilyHealthWarning(id)
}

/**
 * 格式化为后端可接受的 LocalDateTime 查询串（ISO）
 * GET 绑定 LocalDateTime 需 yyyy-MM-ddTHH:mm:ss，空格格式会 400。
 */
export function toMeasuredParam(date, endOfDay = false) {
  if (!date) return undefined
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return undefined
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hms = endOfDay ? '23:59:59' : '00:00:00'
  return `${y}-${m}-${day}T${hms}`
}

/**
 * POST/PUT Body：HealthRecord DTO 的 @JsonFormat 为 yyyy-MM-dd HH:mm:ss
 */
export function formatMeasuredAtBody(input) {
  if (!input) return ''
  return String(input).replace('T', ' ').slice(0, 19)
}

/**
 * 管理端健康记录（P2）
 * GET /api/health-records
 * Query: elderId?, measuredFrom?, measuredTo?, page, size
 */
export function pageHealthRecords(params = {}) {
  return request.get('/health-records', { params })
}

/** GET /api/health-records/{id} — 详情含 warnings */
export function getHealthRecord(id) {
  return request.get(`/health-records/${id}`)
}

/**
 * POST /api/health-records
 * Body: { elderId, measuredAt, systolicPressure?, diastolicPressure?,
 *         bloodGlucose?, bodyTemperature?, heartRate?, remark? }
 */
export function createHealthRecord(data) {
  return request.post('/health-records', data)
}

/**
 * PUT /api/health-records/{id}
 * Body: { measuredAt, systolicPressure?, diastolicPressure?,
 *         bloodGlucose?, bodyTemperature?, heartRate?, remark? }
 * 不可改 elderId
 */
export function updateHealthRecord(id, data) {
  return request.put(`/health-records/${id}`, data)
}

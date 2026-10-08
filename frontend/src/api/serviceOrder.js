import request from './request'

/**
 * POST /api/service-orders
 * Body: { elderId, serviceItemId, scheduledStartTime, scheduledEndTime, remark? }
 * CreateDTO 不含 careStaffId
 */
export function createServiceOrder(data) {
  return request.post('/service-orders', data)
}

/** GET /api/service-orders/{id} */
export function getServiceOrder(id) {
  return request.get(`/service-orders/${id}`)
}

/**
 * GET /api/service-orders
 * Query: elderId?, serviceItemId?, careStaffId?, status?, scheduledStartFrom?, scheduledStartTo?, page, size
 * 返回 PageResult：{ records, total, page, size }
 * FAMILY 仅能看到绑定老人订单（后端隔离）
 */
export function listServiceOrders(params = {}) {
  return request.get('/service-orders', {
    params: {
      page: 1,
      size: 10,
      ...params,
    },
  })
}

/** 管理端别名 */
export function pageServiceOrders(params = {}) {
  return listServiceOrders(params)
}

/**
 * POST /api/service-orders/{id}/confirm
 * Body: { careStaffId } 必填 — 确认同时分配护理员（P4，无独立 assign API）
 * 仅 PENDING → CONFIRMED
 */
export function confirmServiceOrder(id, data) {
  return request.post(`/service-orders/${id}/confirm`, data)
}

/**
 * POST /api/service-orders/{id}/cancel
 * Body: { cancelReason? } 最长 500
 * 仅 PENDING / CONFIRMED
 */
export function cancelServiceOrder(id, data = {}) {
  return request.post(`/service-orders/${id}/cancel`, data)
}

/**
 * POST /api/service-orders/{id}/start
 * CONFIRMED → IN_SERVICE；护理员仅本人订单
 */
export function startServiceOrder(id) {
  return request.post(`/service-orders/${id}/start`)
}

/**
 * POST /api/service-orders/{id}/complete
 * IN_SERVICE → COMPLETED；护理员仅本人订单
 */
export function completeServiceOrder(id) {
  return request.post(`/service-orders/${id}/complete`)
}

/** POST /api/service-orders/{id}/pay — FAMILY 模拟支付 */
export function payServiceOrder(id) {
  return request.post(`/service-orders/${id}/pay`)
}

export function formatOrderDateTime(input) {
  if (!input) return ''
  const d = input instanceof Date ? input : new Date(String(input).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function calcScheduledEnd(startInput, durationMinutes) {
  const start = startInput instanceof Date
    ? startInput
    : new Date(String(startInput).replace(' ', 'T'))
  if (Number.isNaN(start.getTime())) return ''
  const minutes = Number(durationMinutes) || 0
  const end = new Date(start.getTime() + minutes * 60 * 1000)
  return formatOrderDateTime(end)
}

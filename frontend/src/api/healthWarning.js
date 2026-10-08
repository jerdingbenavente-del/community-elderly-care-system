import request from './request'

/**
 * GET /api/health-warnings
 * Query: elderId?, healthRecordId?, status?, indicator?, page, size
 */
export function pageHealthWarnings(params = {}) {
  return request.get('/health-warnings', { params })
}

/** GET /api/health-warnings/{id} */
export function getHealthWarning(id) {
  return request.get(`/health-warnings/${id}`)
}

/**
 * PUT /api/health-warnings/{id}/handle
 * Body: { handlingResult } 必填，最长 500
 * 仅 UNHANDLED → HANDLED；已处理返回 409
 */
export function handleHealthWarning(id, data) {
  return request.put(`/health-warnings/${id}/handle`, data)
}

import request from './request'

/** GET /api/care-service-items — serviceName/serviceCode/serviceType/status/page/size */
export function pageCareServiceItems(params = {}) {
  return request.get('/care-service-items', { params })
}

/** GET /api/care-service-items/{id} */
export function getCareServiceItem(id) {
  return request.get(`/care-service-items/${id}`)
}

/**
 * POST /api/care-service-items
 * Body: { serviceCode, serviceName, serviceType?, description?, durationMinutes, price? }
 * 新建后状态固定为 ENABLED
 */
export function createCareServiceItem(data) {
  return request.post('/care-service-items', data)
}

/**
 * PUT /api/care-service-items/{id}
 * Body: { serviceName, serviceType?, description?, durationMinutes, price?, status }
 * status 必填：ENABLED | DISABLED；不可改 serviceCode
 */
export function updateCareServiceItem(id, data) {
  return request.put(`/care-service-items/${id}`, data)
}

/** DELETE /api/care-service-items/{id} — 逻辑删除；历史订单保留 */
export function deleteCareServiceItem(id) {
  return request.delete(`/care-service-items/${id}`)
}

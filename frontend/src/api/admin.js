import request from './request'

/** GET /api/elders — page 用 total（ADMIN: elder:list） */
export function pageElders(params = {}) {
  return request.get('/elders', { params })
}

/** GET /api/care-staff */
export function pageCareStaff(params = {}) {
  return request.get('/care-staff', { params })
}

/** GET /api/service-orders */
export function pageServiceOrders(params = {}) {
  return request.get('/service-orders', { params })
}

/** GET /api/health-warnings */
export function pageHealthWarnings(params = {}) {
  return request.get('/health-warnings', { params })
}

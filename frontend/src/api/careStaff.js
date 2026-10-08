import request from './request'

/** GET /api/care-staff — page；employeeNo/name/phone/status/userId/page/size */
export function pageCareStaff(params = {}) {
  return request.get('/care-staff', { params })
}

/** GET /api/care-staff/{id} */
export function getCareStaff(id) {
  return request.get(`/care-staff/${id}`)
}

/**
 * POST /api/care-staff
 * Body: { userId, employeeNo, name, gender?, phone?, position?, remark? }
 */
export function createCareStaff(data) {
  return request.post('/care-staff', data)
}

/** GET /api/care-staff/me — 当前登录护理员业务身份 */
export function getMyCareStaff() {
  return request.get('/care-staff/me')
}

/**
 * PUT /api/care-staff/{id}
 * Body: { name, gender?, phone?, position?, remark? }
 * 不可改 userId / employeeNo / status
 */
export function updateCareStaff(id, data) {
  return request.put(`/care-staff/${id}`, data)
}

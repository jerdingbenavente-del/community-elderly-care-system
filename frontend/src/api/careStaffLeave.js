import request from './request'

/** GET /api/care-staff-leaves/my */
export function pageMyLeaves(params = {}) {
  return request.get('/care-staff-leaves/my', { params })
}

/** GET /api/care-staff-leaves/admin */
export function pageAdminLeaves(params = {}) {
  return request.get('/care-staff-leaves/admin', { params })
}

/** GET /api/care-staff-leaves/{id} */
export function getLeave(id) {
  return request.get(`/care-staff-leaves/${id}`)
}

/** POST /api/care-staff-leaves */
export function createLeave(data) {
  return request.post('/care-staff-leaves', data)
}

/** DELETE /api/care-staff-leaves/{id} */
export function cancelLeave(id) {
  return request.delete(`/care-staff-leaves/${id}`)
}

/** POST /api/care-staff-leaves/{id}/approve */
export function approveLeave(id, data = {}) {
  return request.post(`/care-staff-leaves/${id}/approve`, data)
}

/** POST /api/care-staff-leaves/{id}/reject */
export function rejectLeave(id, data = {}) {
  return request.post(`/care-staff-leaves/${id}/reject`, data)
}

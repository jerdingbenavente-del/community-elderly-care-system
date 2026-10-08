import request from './request'

/**
 * GET /api/care-staff-schedules
 * Query: careStaffId?, scheduleDate?, startDate?, endDate?, status?, page, size
 */
export function pageCareStaffSchedules(params = {}) {
  return request.get('/care-staff-schedules', { params })
}

/** GET /api/care-staff-schedules/{id} */
export function getCareStaffSchedule(id) {
  return request.get(`/care-staff-schedules/${id}`)
}

/**
 * POST /api/care-staff-schedules
 * Body: { careStaffId, scheduleDate, startTime, endTime, remark? }
 * 新建后状态固定 AVAILABLE
 */
export function createCareStaffSchedule(data) {
  return request.post('/care-staff-schedules', data)
}

/**
 * PUT /api/care-staff-schedules/{id}
 * Body: { scheduleDate, startTime, endTime, status, remark? }
 * status: AVAILABLE | CANCELLED；不可改 careStaffId
 */
export function updateCareStaffSchedule(id, data) {
  return request.put(`/care-staff-schedules/${id}`, data)
}

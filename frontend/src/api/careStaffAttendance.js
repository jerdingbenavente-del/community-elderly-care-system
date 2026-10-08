import request from './request'

/** GET /api/care-staff-attendance/today */
export function getTodayAttendance() {
  return request.get('/care-staff-attendance/today')
}

/** POST /api/care-staff-attendance/check-in */
export function checkInAttendance() {
  return request.post('/care-staff-attendance/check-in')
}

/** POST /api/care-staff-attendance/check-out */
export function checkOutAttendance() {
  return request.post('/care-staff-attendance/check-out')
}

/** GET /api/care-staff-attendance/my */
export function pageMyAttendance(params = {}) {
  return request.get('/care-staff-attendance/my', { params })
}

/** GET /api/care-staff-attendance/admin */
export function pageAdminAttendance(params = {}) {
  return request.get('/care-staff-attendance/admin', { params })
}

import request from './request'

export function pageAdminMedications(params = {}) {
  return request.get('/elder-medications/admin', { params })
}

export function pageFamilyMedications(params = {}) {
  return request.get('/elder-medications/family', { params })
}

export function pageStaffMedications(params = {}) {
  return request.get('/elder-medications/staff', { params })
}

export function todayMedicationReminders() {
  return request.get('/elder-medications/today-reminders')
}

export function createMedication(data) {
  return request.post('/elder-medications', data)
}

export function updateMedication(id, data) {
  return request.put(`/elder-medications/${id}`, data)
}

export function enableMedication(id) {
  return request.post(`/elder-medications/${id}/enable`)
}

export function disableMedication(id) {
  return request.post(`/elder-medications/${id}/disable`)
}

export function deleteMedication(id) {
  return request.delete(`/elder-medications/${id}`)
}

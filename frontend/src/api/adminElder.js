import request from './request'

/** GET /api/elders — 分页；name/phone/status/page/size */
export function pageElders(params = {}) {
  return request.get('/elders', { params })
}

/** GET /api/elders/{id} */
export function getElder(id) {
  return request.get(`/elders/${id}`)
}

/** POST /api/elders */
export function createElder(data) {
  return request.post('/elders', data)
}

/** PUT /api/elders/{id} */
export function updateElder(id, data) {
  return request.put(`/elders/${id}`, data)
}

/** GET /api/elders/{elderId}/emergency-contacts */
export function listEmergencyContacts(elderId) {
  return request.get(`/elders/${elderId}/emergency-contacts`)
}

/** POST /api/elders/{elderId}/emergency-contacts */
export function createEmergencyContact(elderId, data) {
  return request.post(`/elders/${elderId}/emergency-contacts`, data)
}

/** PUT /api/emergency-contacts/{id} */
export function updateEmergencyContact(id, data) {
  return request.put(`/emergency-contacts/${id}`, data)
}

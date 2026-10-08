import request from './request'

export function pageAdminActivities(params) {
  return request.get('/activities/admin', { params })
}

export function pagePublicActivities(params) {
  return request.get('/activities', { params })
}

export function getActivity(id) {
  return request.get(`/activities/${id}`)
}

export function createActivity(data) {
  return request.post('/activities', data)
}

export function updateActivity(id, data) {
  return request.put(`/activities/${id}`, data)
}

export function publishActivity(id) {
  return request.post(`/activities/${id}/publish`)
}

export function cancelActivity(id) {
  return request.post(`/activities/${id}/cancel`)
}

export function deleteActivity(id) {
  return request.delete(`/activities/${id}`)
}

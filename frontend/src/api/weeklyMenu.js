import request from './request'

export function listWeeklyMenus() {
  return request.get('/weekly-menus')
}

export function getWeeklyMenu(weekStart) {
  return request.get('/weekly-menus/by-week', { params: { weekStart } })
}

export function getElderMenu(elderId, weekStart) {
  return request.get('/weekly-menus/elder-view', { params: { elderId, weekStart } })
}

export function staffTodayMenus() {
  return request.get('/weekly-menus/staff-today')
}

export function createWeeklyMenu(data) {
  return request.post('/weekly-menus', data)
}

export function updateWeeklyMenu(id, data) {
  return request.put(`/weekly-menus/${id}`, data)
}

export function deleteWeeklyMenu(id) {
  return request.delete(`/weekly-menus/${id}`)
}

export function adminDietaryNotes() {
  return request.get('/dietary-notes/admin')
}

export function getDietaryNote(elderId) {
  return request.get('/dietary-notes', { params: { elderId } })
}

export function saveDietaryNote(data) {
  return request.put('/dietary-notes', data)
}

export function saveMealAdjustment(data) {
  return request.post('/meal-adjustments', data)
}

export function cancelMealAdjustment(id) {
  return request.delete(`/meal-adjustments/${id}`)
}

import request from './request'

/** GET /api/family/elders — 当前家属绑定的老人列表 */
export function listMyElders() {
  return request.get('/family/elders')
}

/** GET /api/family/elders/{elderId} — 绑定校验后的老人详情 */
export function getMyElder(elderId) {
  return request.get(`/family/elders/${elderId}`)
}

/** GET /api/family/elders/{elderId}/emergency-contacts */
export function listElderContacts(elderId) {
  return request.get(`/family/elders/${elderId}/emergency-contacts`)
}

/** 别名：与 F3 规范命名对齐 */
export function getElderList() {
  return listMyElders()
}

export function getElderDetail(elderId) {
  return getMyElder(elderId)
}

export function getEmergencyContacts(elderId) {
  return listElderContacts(elderId)
}

/**
 * 管理端绑定列表：GET /api/elder-families
 * 注意：后端 denyFamilyOnAdminApi，纯 FAMILY 会 403，家属端勿调用。
 */
export function getFamilyBindings(params = {}) {
  return request.get('/elder-families', { params })
}

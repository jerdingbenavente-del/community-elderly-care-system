import request from './request'

/** GET /api/system/users — 分页；支持 username/realName/status/roleCode */
export function pageSystemUsers(params = {}) {
  return request.get('/system/users', { params })
}

/** GET /api/system/users/{id} */
export function getSystemUser(id) {
  return request.get(`/system/users/${id}`)
}

/**
 * POST /api/system/users/business-accounts
 * Body: { name, roleCode } — 后端自动生成 username + 初始密码 123456
 */
export function createBusinessAccount(data) {
  return request.post('/system/users/business-accounts', data)
}

/** POST /api/system/users/{id}/enable */
export function enableSystemUser(id) {
  return request.post(`/system/users/${id}/enable`)
}

/** POST /api/system/users/{id}/disable */
export function disableSystemUser(id) {
  return request.post(`/system/users/${id}/disable`)
}

/** DELETE /api/system/users/{id} — 逻辑删除 */
export function deleteSystemUser(id) {
  return request.delete(`/system/users/${id}`)
}

/** PUT /api/system/users/me — 当前用户改姓名/手机 */
export function updateMyProfile(data) {
  return request.put('/system/users/me', data)
}

/** POST /api/system/users/me/avatar — multipart field: file */
export function uploadMyAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  // 不要手动设 Content-Type，由浏览器带 boundary
  return request.post('/system/users/me/avatar', form)
}

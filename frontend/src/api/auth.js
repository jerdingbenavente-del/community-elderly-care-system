import request from './request'

/**
 * 真实后端：POST /api/auth/login
 * Body: { username, password }
 * data: { token, userId, username, roles, permissions, mustChangePassword }
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** GET /api/auth/me — 当前登录用户（不含 token） */
export function getAuthMe() {
  return request.get('/auth/me')
}

/**
 * GET /api/system/users/me — 含 realName 等资料（isAuthenticated）
 */
export function getCurrentUserProfile() {
  return request.get('/system/users/me')
}

/**
 * POST /api/system/users/change-password
 * Body: { oldPassword, newPassword, confirmPassword }
 */
export function changePassword(data) {
  return request.post('/system/users/change-password', data)
}

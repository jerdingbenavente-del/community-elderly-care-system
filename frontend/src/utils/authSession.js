/**
 * 会话持久化与 JWT 角色权威读取。
 * 解决多标签登录竞态：localStorage.roles 写成 ADMIN，但 token 已被另一标签写成 FAMILY，
 * 导致管理端 UI 仍按 ADMIN 渲染，Axios 却带 FAMILY token → 后端 403「无权限访问」。
 */

const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'
const ROLES_KEY = 'roles'
const MUST_CHANGE_PASSWORD_KEY = 'mustChangePassword'
const SESSION_KEY = 'authSession'

export const AUTH_STORAGE_KEYS = [
  TOKEN_KEY,
  USER_KEY,
  ROLES_KEY,
  MUST_CHANGE_PASSWORD_KEY,
  SESSION_KEY,
]

export function normalizeRoles(list) {
  if (!Array.isArray(list)) return []
  return list
    .map((item) => {
      if (typeof item === 'string') return item
      if (item && typeof item === 'object') {
        return item.roleCode || item.code || item.role || ''
      }
      return ''
    })
    .filter(Boolean)
}

/** 解析 JWT payload（不校验签名；签名由后端负责） */
export function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const json = atob(padded)
    // 处理中文等 UTF-8 字符
    const decoded = decodeURIComponent(
      Array.from(json, (c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''),
    )
    return JSON.parse(decoded)
  } catch {
    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
      return JSON.parse(atob(padded))
    } catch {
      return null
    }
  }
}

/** 角色以 JWT claims.roles 为准，避免与 token 脱节 */
export function rolesFromToken(token, fallbackRoles = []) {
  const payload = parseJwtPayload(token)
  const fromJwt = normalizeRoles(payload?.roles)
  if (fromJwt.length) return fromJwt
  return normalizeRoles(fallbackRoles)
}

export function readLegacySession() {
  let userInfo = {}
  try {
    userInfo = JSON.parse(localStorage.getItem(USER_KEY) || '{}') || {}
  } catch {
    userInfo = {}
  }
  let roles = []
  try {
    roles = normalizeRoles(JSON.parse(localStorage.getItem(ROLES_KEY) || '[]'))
  } catch {
    roles = []
  }
  return {
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo,
    roles,
    mustChangePassword:
      localStorage.getItem(MUST_CHANGE_PASSWORD_KEY) === 'true'
      || localStorage.getItem(MUST_CHANGE_PASSWORD_KEY) === '1',
  }
}

export function readAuthSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object' && parsed.token) {
        const token = String(parsed.token || '')
        return {
          token,
          userInfo: parsed.userInfo || {},
          roles: rolesFromToken(token, parsed.roles),
          mustChangePassword: Boolean(parsed.mustChangePassword),
        }
      }
    }
  } catch {
    // fall through to legacy
  }
  const legacy = readLegacySession()
  return {
    ...legacy,
    roles: rolesFromToken(legacy.token, legacy.roles),
  }
}

/** 原子写入：先写聚合 session，再同步旧键，降低跨标签撕裂窗口 */
export function writeAuthSession({
  token = '',
  userInfo = {},
  roles = [],
  mustChangePassword = false,
} = {}) {
  const normalizedRoles = rolesFromToken(token, roles)
  const session = {
    v: 1,
    token: token || '',
    userInfo: userInfo || {},
    roles: normalizedRoles,
    mustChangePassword: Boolean(mustChangePassword),
    updatedAt: Date.now(),
  }
  if (!session.token) {
    clearAuthSession()
    return session
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(USER_KEY, JSON.stringify(session.userInfo))
  localStorage.setItem(ROLES_KEY, JSON.stringify(session.roles))
  localStorage.setItem(MUST_CHANGE_PASSWORD_KEY, session.mustChangePassword ? 'true' : 'false')
  return session
}

export function clearAuthSession() {
  AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key))
}

export {
  TOKEN_KEY,
  USER_KEY,
  ROLES_KEY,
  MUST_CHANGE_PASSWORD_KEY,
  SESSION_KEY,
}

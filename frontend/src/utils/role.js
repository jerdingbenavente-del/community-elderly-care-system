/**
 * 角色首页优先级：ADMIN > FAMILY > CARE_STAFF
 * （FAMILY+CARE_STAFF 默认进家属端）
 */
export function resolveHomeByRoles(roles = []) {
  const set = new Set(roles || [])
  if (set.has('ADMIN')) return '/admin'
  if (set.has('FAMILY')) return '/family'
  if (set.has('CARE_STAFF')) return '/staff'
  return '/login'
}

export function hasAnyRole(userRoles = [], requiredRoles = []) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  const set = new Set(userRoles || [])
  return requiredRoles.some((r) => set.has(r))
}

export function canAccessPath(path, roles = []) {
  if (!path) return false
  if (path.startsWith('/admin')) return hasAnyRole(roles, ['ADMIN'])
  if (path.startsWith('/staff')) return hasAnyRole(roles, ['CARE_STAFF'])
  if (path.startsWith('/family')) return hasAnyRole(roles, ['FAMILY'])
  return true
}

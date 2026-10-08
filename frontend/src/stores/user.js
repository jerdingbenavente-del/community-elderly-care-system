import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAuthMe, getCurrentUserProfile, login as loginApi } from '@/api/auth'
import { canAccessPath, resolveHomeByRoles } from '@/utils/role'
import {
  clearAuthSession,
  normalizeRoles,
  readAuthSession,
  rolesFromToken,
  writeAuthSession,
  AUTH_STORAGE_KEYS,
} from '@/utils/authSession'

/**
 * 用户会话：JWT + 角色（不持久化密码）
 * 角色以 JWT claims 为准，避免多标签登录导致 token/roles 撕裂。
 */
export const useUserStore = defineStore('user', () => {
  const initial = readAuthSession()
  const token = ref(initial.token || '')
  const userInfo = ref(initial.userInfo || {})
  const roles = ref(normalizeRoles(initial.roles || []))
  const mustChangePassword = ref(Boolean(initial.mustChangePassword))
  /** 同页连续登录世代号，避免旧 login await 回写覆盖新会话 */
  let loginGeneration = 0

  const displayName = computed(() => {
    return userInfo.value?.realName || userInfo.value?.username || '用户'
  })

  const avatarUrl = computed(() => userInfo.value?.avatar || '')

  const isLoggedIn = computed(() => Boolean(token.value))

  function persist() {
    writeAuthSession({
      token: token.value,
      userInfo: userInfo.value,
      roles: roles.value,
      mustChangePassword: mustChangePassword.value,
    })
  }

  /** 从 localStorage 重新灌入；角色强制对齐 JWT */
  function hydrateFromStorage() {
    const session = readAuthSession()
    token.value = session.token || ''
    userInfo.value = session.userInfo || {}
    roles.value = normalizeRoles(session.roles || [])
    mustChangePassword.value = Boolean(session.mustChangePassword)
    if (token.value) {
      // 回写校正后的 roles，修复历史撕裂数据
      persist()
    }
    return session
  }

  function setToken(value) {
    token.value = value || ''
    if (token.value) {
      roles.value = rolesFromToken(token.value, roles.value)
    }
    persist()
  }

  function setUserInfo(info) {
    userInfo.value = info || {}
    persist()
  }

  /** 用 /system/users/me 结果同步本地展示字段 */
  function applyProfile(data) {
    if (!data) return
    setUserInfo({
      ...userInfo.value,
      userId: data.id ?? userInfo.value.userId,
      username: data.username ?? userInfo.value.username,
      realName: data.realName,
      phone: data.phone,
      avatar: data.avatar || '',
      permissions: userInfo.value.permissions || [],
    })
    // 角色仍以当前 JWT 为准，避免 profile 无 permissions/roles 时被空数组覆盖
    const nextRoles = rolesFromToken(token.value, data.roles)
    if (nextRoles.length) {
      setRoles(nextRoles)
    }
    if (typeof data.mustChangePassword === 'boolean') {
      setMustChangePassword(data.mustChangePassword)
    }
  }

  async function refreshProfile() {
    const profileRes = await getCurrentUserProfile()
    if (profileRes?.code === 200 && profileRes.data) {
      applyProfile(profileRes.data)
    }
    return profileRes
  }

  /** 用 /auth/me 校正 roles + permissions（后端 DB 权威权限） */
  async function syncSessionFromMe() {
    if (!token.value) return null
    const res = await getAuthMe()
    if (res?.code === 200 && res.data) {
      const data = res.data
      roles.value = rolesFromToken(token.value, data.roles || [])
      userInfo.value = {
        ...userInfo.value,
        userId: data.userId ?? userInfo.value.userId,
        username: data.username ?? userInfo.value.username,
        permissions: data.permissions || userInfo.value.permissions || [],
      }
      if (typeof data.mustChangePassword === 'boolean') {
        mustChangePassword.value = data.mustChangePassword
      }
      persist()
    }
    return res
  }

  function setRoles(list) {
    roles.value = rolesFromToken(token.value, list)
    persist()
  }

  function setMustChangePassword(value) {
    mustChangePassword.value = Boolean(value)
    persist()
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  function getPrimaryRole() {
    if (hasRole('ADMIN')) return 'ADMIN'
    if (hasRole('FAMILY')) return 'FAMILY'
    if (hasRole('CARE_STAFF')) return 'CARE_STAFF'
    return null
  }

  function getHomePath() {
    return resolveHomeByRoles(roles.value)
  }

  /**
   * 多角色切换骨架：返回目标路径（无权限时返回 null）
   */
  function switchRole(targetRole) {
    if (!hasRole(targetRole)) {
      return null
    }
    const map = {
      ADMIN: '/admin',
      FAMILY: '/family',
      CARE_STAFF: '/staff',
    }
    return map[targetRole] || null
  }

  async function login(username, password) {
    // 先作废进行中的旧 login，再清空字段（不再次 ++，避免取消自己）
    loginGeneration += 1
    const generation = loginGeneration
    token.value = ''
    userInfo.value = {}
    roles.value = []
    mustChangePassword.value = false
    clearAuthSession()

    const res = await loginApi({ username, password })
    if (generation !== loginGeneration) {
      throw new Error('登录已取消')
    }
    if (!res || res.code !== 200 || !res.data?.token) {
      throw new Error(res?.message || '登录失败')
    }
    const data = res.data
    token.value = data.token
    roles.value = rolesFromToken(data.token, data.roles || [])
    mustChangePassword.value = Boolean(data.mustChangePassword)
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      permissions: data.permissions || [],
    }
    persist()

    // 补充 realName / avatar（登录 VO 不含这些字段；P8 白名单允许 /system/users/me）
    try {
      const profileRes = await getCurrentUserProfile()
      if (generation !== loginGeneration) {
        throw new Error('登录已取消')
      }
      if (profileRes?.code === 200 && profileRes.data) {
        userInfo.value = {
          userId: data.userId,
          username: data.username,
          realName: profileRes.data.realName,
          phone: profileRes.data.phone,
          avatar: profileRes.data.avatar || '',
          permissions: data.permissions || [],
        }
        if (typeof profileRes.data.mustChangePassword === 'boolean') {
          mustChangePassword.value = profileRes.data.mustChangePassword
        }
        // 再次以 JWT 校准角色
        roles.value = rolesFromToken(token.value, roles.value)
        persist()
      }
    } catch (e) {
      if (e?.message === '登录已取消') throw e
      // 资料拉取失败不影响登录成功
    }

    return data
  }

  function resetState() {
    // 退出/强制清理时作废进行中的 login，防止旧 await 回写
    loginGeneration += 1
    token.value = ''
    userInfo.value = {}
    roles.value = []
    mustChangePassword.value = false
    clearAuthSession()
  }

  function logout() {
    resetState()
  }

  return {
    token,
    userInfo,
    roles,
    mustChangePassword,
    displayName,
    avatarUrl,
    isLoggedIn,
    hydrateFromStorage,
    syncSessionFromMe,
    setToken,
    setUserInfo,
    applyProfile,
    refreshProfile,
    setRoles,
    setMustChangePassword,
    hasRole,
    getPrimaryRole,
    getHomePath,
    switchRole,
    login,
    logout,
    resetState,
  }
})

/**
 * 跨标签会话同步：其他页登录/退出后，本页立即对齐 JWT 角色并离开无权限路由。
 */
export function bindCrossTabSessionSync(router) {
  if (typeof window === 'undefined') return
  window.addEventListener('storage', (event) => {
    if (event.key && !AUTH_STORAGE_KEYS.includes(event.key)) return
    const userStore = useUserStore()
    userStore.hydrateFromStorage()
    const path = router.currentRoute.value?.path || ''
    if (!userStore.token) {
      if (path !== '/login') {
        router.replace({ path: '/login', query: { redirect: path } })
      }
      return
    }
    if (path && !canAccessPath(path, userStore.roles)) {
      const home = userStore.getHomePath()
      if (home && home !== path) {
        router.replace(home)
      }
    }
  })
}

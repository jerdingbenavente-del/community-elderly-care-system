import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

let handling401 = false
let handlingMustChange = false

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      // 与后端 JwtAuthenticationFilter 一致：Authorization: Bearer <token>
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

/**
 * 统一业务错误：拦截器已提示时 toastShown=true，页面勿再 ElMessage。
 */
function rejectWithCode(message, code, toastShown = false) {
  const err = new Error(message || '请求失败')
  err.code = code
  err.toastShown = toastShown
  return Promise.reject(err)
}

/** 页面 catch 用：仅当拦截器未提示时再弹一次 */
export function toastIfNeeded(error, fallbackMessage, type = 'error') {
  if (error?.toastShown) return
  const msg = error?.message || fallbackMessage || '操作失败'
  if (type === 'warning') ElMessage.warning(msg)
  else ElMessage.error(msg)
}

function isMustChangePasswordForbidden(message) {
  return typeof message === 'string' && message.includes('首次登录请先修改密码')
}

async function handleMustChangePassword(message) {
  if (handlingMustChange) return
  handlingMustChange = true
  try {
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    if (!userStore.mustChangePassword && !isMustChangePasswordForbidden(message)) {
      return
    }
    userStore.setMustChangePassword(true)
    const { default: router } = await import('@/router')
    if (router.currentRoute.value.path !== '/change-password') {
      ElMessage.warning(message || '首次登录请先修改密码')
      await router.replace('/change-password')
    }
  } finally {
    handlingMustChange = false
  }
}

/**
 * 管理端 403 且当前 JWT 实际不是 ADMIN：多半是多标签登录把 token 覆盖了。
 * 纠正会话并跳转登录，避免页面仍显示管理端却反复弹出「无权限访问」。
 * @returns {Promise<boolean>} 是否已按会话撕裂处理
 */
async function handleStaleAdminSession(message) {
  try {
    const path = window.location?.pathname || ''
    if (!path.startsWith('/admin')) return false
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    userStore.hydrateFromStorage()
    if (userStore.hasRole('ADMIN')) return false
    ElMessage.warning(message || '登录状态已变更，请重新登录管理端')
    userStore.resetState()
    const { default: router } = await import('@/router')
    if (router.currentRoute.value.path !== '/login') {
      await router.replace({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
    }
    return true
  } catch {
    return false
  }
}

request.interceptors.response.use(
  async (response) => {
    const res = response.data
    if (res && typeof res.code === 'number' && res.code !== 200) {
      const message = res.message || '请求失败'
      let toasted = true
      if (res.code === 401) {
        handleUnauthorized(message)
      } else if (res.code === 403) {
        if (isMustChangePasswordForbidden(message) || localStorage.getItem('mustChangePassword') === 'true') {
          handleMustChangePassword(message)
        } else if (await handleStaleAdminSession(message)) {
          // 多标签会话撕裂：已引导重新登录
        } else {
          ElMessage.warning(message || '无权限访问')
        }
      } else if (res.code === 404) {
        ElMessage.warning(message || '资源不存在')
      } else if (res.code === 409) {
        ElMessage.warning(message || '业务冲突，请稍后重试')
      } else if (res.code === 400) {
        ElMessage.warning(message || '请检查请求参数')
      } else if (res.code === 500) {
        ElMessage.error(message || '系统服务异常，请稍后重试')
      } else {
        ElMessage.warning(message)
      }
      return rejectWithCode(message, res.code, toasted)
    }
    return res
  },
  async (error) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message || error.message || '网络请求失败'

    let toasted = false
    if (status === 401) {
      handleUnauthorized(message)
      toasted = true
    } else if (status === 403) {
      if (isMustChangePasswordForbidden(message) || localStorage.getItem('mustChangePassword') === 'true') {
        handleMustChangePassword(message)
      } else if (await handleStaleAdminSession(message)) {
        // handled
      } else {
        ElMessage.warning(message || '无权限访问')
      }
      toasted = true
    } else if (status === 404) {
      ElMessage.warning(message || '资源不存在')
      toasted = true
    } else if (status === 409) {
      ElMessage.warning(message || '业务冲突，请稍后重试')
      toasted = true
    } else if (status === 400) {
      ElMessage.warning(message || '请检查请求参数')
      toasted = true
    } else if (status === 500) {
      ElMessage.error(message || '系统服务异常，请稍后重试')
      toasted = true
    } else if (!status) {
      ElMessage.error(message || '网络请求失败')
      toasted = true
    }

    return rejectWithCode(message, status, toasted)
  },
)

async function handleUnauthorized(message) {
  if (handling401) return
  handling401 = true
  try {
    const { useUserStore } = await import('@/stores/user')
    useUserStore().resetState()
  } catch {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('roles')
    localStorage.removeItem('mustChangePassword')
    localStorage.removeItem('authSession')
  }

  try {
    const { default: router } = await import('@/router')
    const current = router.currentRoute.value
    if (current.path !== '/login') {
      ElMessage.error(message || '登录已失效，请重新登录')
      await router.replace({
        path: '/login',
        query: { redirect: current.fullPath },
      })
    }
  } finally {
    handling401 = false
  }
}

export default request

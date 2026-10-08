import { createRouter, createWebHistory } from 'vue-router'
import FamilyLayout from '@/layouts/FamilyLayout.vue'
import StaffLayout from '@/layouts/StaffLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useUserStore } from '@/stores/user'
import { hasAnyRole } from '@/utils/role'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', requiresAuth: false },
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('@/views/auth/ChangePasswordView.vue'),
      meta: { title: '修改密码', requiresAuth: true },
    },
    {
      path: '/family',
      component: FamilyLayout,
      meta: { requiresAuth: true, roles: ['FAMILY'] },
      children: [
        {
          path: '',
          name: 'family-home',
          component: () => import('@/views/family/FamilyHomeView.vue'),
          meta: { title: '家属中心' },
        },
        {
          path: 'elders',
          name: 'family-elders',
          component: () => import('@/views/family/FamilyElderListView.vue'),
          meta: { title: '我的老人' },
        },
        {
          path: 'elders/:id',
          name: 'family-elder-detail',
          component: () => import('@/views/family/FamilyElderDetailView.vue'),
          meta: { title: '老人档案' },
        },
        {
          path: 'health',
          name: 'family-health',
          component: () => import('@/views/family/FamilyHealthView.vue'),
          meta: { title: '健康管理' },
        },
        {
          path: 'care-services',
          name: 'family-care-services',
          component: () => import('@/views/family/FamilyCareServiceView.vue'),
          meta: { title: '照护服务' },
        },
        {
          path: 'care-services/:id',
          name: 'family-care-service-detail',
          component: () => import('@/views/family/FamilyCareServiceDetailView.vue'),
          meta: { title: '服务详情' },
        },
        {
          path: 'care-services/:id/book',
          name: 'family-care-service-book',
          component: () => import('@/views/family/FamilyServiceBookingView.vue'),
          meta: { title: '服务预约' },
        },
        {
          path: 'services',
          redirect: '/family/care-services',
        },
        {
          path: 'orders',
          name: 'family-orders',
          component: () => import('@/views/family/FamilyOrdersView.vue'),
          meta: { title: '我的订单' },
        },
        {
          path: 'orders/:id/pay',
          name: 'family-order-pay',
          component: () => import('@/views/family/FamilyOrderPayView.vue'),
          meta: { title: '支付订单' },
        },
        {
          path: 'evaluations',
          name: 'family-evaluations',
          component: () => import('@/views/family/FamilyEvaluationsView.vue'),
          meta: { title: '服务评价' },
        },
        {
          path: 'menu',
          name: 'family-menu',
          component: () => import('@/views/family/FamilyMenuView.vue'),
          meta: { title: '膳食菜单' },
        },
        {
          path: 'activities',
          name: 'family-activities',
          component: () => import('@/views/family/FamilyActivitiesView.vue'),
          meta: { title: '社区活动' },
        },
        {
          path: 'profile',
          name: 'family-profile',
          component: () => import('@/views/family/FamilyProfileView.vue'),
          meta: { title: '我的' },
        },
      ],
    },
    {
      path: '/staff',
      component: StaffLayout,
      meta: { requiresAuth: true, roles: ['CARE_STAFF'] },
      children: [
        {
          path: '',
          name: 'staff-home',
          component: () => import('@/views/staff/StaffHomeView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'services',
          name: 'staff-services',
          component: () => import('@/views/staff/StaffMyServicesView.vue'),
          meta: { title: '我的服务' },
        },
        {
          path: 'services/:id',
          name: 'staff-service-detail',
          component: () => import('@/views/staff/StaffServiceOrderDetailView.vue'),
          meta: { title: '服务订单详情' },
        },
        {
          path: 'service-records',
          name: 'staff-service-records',
          component: () => import('@/views/staff/StaffServiceRecordsView.vue'),
          meta: { title: '服务记录' },
        },
        {
          path: 'schedules',
          name: 'staff-schedules',
          component: () => import('@/views/staff/StaffMySchedulesView.vue'),
          meta: { title: '我的排班' },
        },
        {
          path: 'leaves',
          name: 'staff-leaves',
          component: () => import('@/views/staff/StaffLeaveView.vue'),
          meta: { title: '临时请假' },
        },
        {
          path: 'attendance',
          name: 'staff-attendance',
          component: () => import('@/views/staff/StaffAttendanceView.vue'),
          meta: { title: '我的考勤' },
        },
        {
          path: 'menu',
          name: 'staff-menu',
          component: () => import('@/views/staff/StaffMenuView.vue'),
          meta: { title: '今日膳食' },
        },
        {
          path: 'activities',
          name: 'staff-activities',
          component: () => import('@/views/staff/StaffActivitiesView.vue'),
          meta: { title: '社区活动' },
        },
        {
          path: 'orders',
          redirect: '/staff/services',
        },
        {
          path: 'evaluations',
          name: 'staff-evaluations',
          component: () => import('@/views/common/ComingSoonView.vue'),
          meta: {
            title: '服务评价',
            description: '护理员端服务评价将在后续阶段开放。',
          },
        },
        {
          path: 'profile',
          name: 'staff-profile',
          component: () => import('@/views/staff/StaffProfileView.vue'),
          meta: { title: '个人中心' },
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard',
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'statistics',
          name: 'admin-statistics',
          component: () => import('@/views/admin/AdminStatisticsView.vue'),
          meta: { title: '运营统计' },
        },
        {
          path: 'elders',
          name: 'admin-elders',
          component: () => import('@/views/admin/AdminEldersView.vue'),
          meta: {
            title: '老人管理',
          },
        },
        {
          path: 'elders/:id',
          name: 'admin-elder-detail',
          component: () => import('@/views/admin/AdminElderDetailView.vue'),
          meta: {
            title: '老人详情',
          },
        },
        {
          path: 'staff',
          name: 'admin-staff',
          component: () => import('@/views/admin/AdminCareStaffView.vue'),
          meta: {
            title: '护理员管理',
          },
        },
        {
          path: 'staff/:id',
          name: 'admin-staff-detail',
          component: () => import('@/views/admin/AdminCareStaffDetailView.vue'),
          meta: {
            title: '护理员详情',
          },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersView.vue'),
          meta: {
            title: '用户管理',
          },
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/views/admin/AdminProfileView.vue'),
          meta: {
            title: '个人中心',
          },
        },
        {
          path: 'health',
          name: 'admin-health',
          component: () => import('@/views/admin/AdminHealthRecordsView.vue'),
          meta: {
            title: '健康档案',
          },
        },
        {
          path: 'health/:id',
          name: 'admin-health-detail',
          component: () => import('@/views/admin/AdminHealthRecordDetailView.vue'),
          meta: {
            title: '健康记录详情',
          },
        },
        {
          path: 'warnings',
          name: 'admin-warnings',
          component: () => import('@/views/admin/AdminHealthWarningsView.vue'),
          meta: {
            title: '健康预警管理',
          },
        },
        {
          path: 'warnings/:id',
          name: 'admin-warning-detail',
          component: () => import('@/views/admin/AdminHealthWarningDetailView.vue'),
          meta: {
            title: '预警详情',
          },
        },
        {
          path: 'services',
          name: 'admin-services',
          component: () => import('@/views/admin/AdminCareServiceItemsView.vue'),
          meta: {
            title: '照护服务管理',
          },
        },
        {
          path: 'services/:id',
          name: 'admin-service-detail',
          component: () => import('@/views/admin/AdminCareServiceItemDetailView.vue'),
          meta: {
            title: '服务项目详情',
          },
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/AdminServiceOrdersView.vue'),
          meta: {
            title: '服务订单管理',
          },
        },
        {
          path: 'orders/:id',
          name: 'admin-order-detail',
          component: () => import('@/views/admin/AdminServiceOrderDetailView.vue'),
          meta: {
            title: '订单详情',
          },
        },
        {
          path: 'schedules',
          name: 'admin-schedules',
          component: () => import('@/views/admin/AdminSchedulesView.vue'),
          meta: {
            title: '排班管理',
          },
        },
        {
          path: 'schedules/:id',
          name: 'admin-schedule-detail',
          component: () => import('@/views/admin/AdminScheduleDetailView.vue'),
          meta: {
            title: '排班详情',
          },
        },
        {
          path: 'evaluations',
          name: 'admin-evaluations',
          component: () => import('@/views/admin/AdminCareEvaluationsView.vue'),
          meta: {
            title: '服务评价管理',
          },
        },
        {
          path: 'evaluations/:id',
          name: 'admin-evaluation-detail',
          component: () => import('@/views/admin/AdminCareEvaluationDetailView.vue'),
          meta: {
            title: '评价详情',
          },
        },
        {
          path: 'leaves',
          name: 'admin-leaves',
          component: () => import('@/views/admin/AdminLeaveApprovalsView.vue'),
          meta: {
            title: '护理员请假审批',
          },
        },
        {
          path: 'attendance',
          name: 'admin-attendance',
          component: () => import('@/views/admin/AdminAttendanceView.vue'),
          meta: {
            title: '护理员考勤',
          },
        },
        {
          path: 'medications',
          name: 'admin-medications',
          component: () => import('@/views/admin/AdminMedicationsView.vue'),
          meta: {
            title: '老人用药',
          },
        },
        {
          path: 'menus',
          name: 'admin-menus',
          component: () => import('@/views/admin/AdminMenusView.vue'),
          meta: {
            title: '每周膳食菜单',
          },
        },
        {
          path: 'elder-diet',
          name: 'admin-elder-diet',
          component: () => import('@/views/admin/AdminElderDietView.vue'),
          meta: {
            title: '老人饮食管理',
          },
        },
        {
          path: 'activities',
          name: 'admin-activities',
          component: () => import('@/views/admin/AdminActivitiesView.vue'),
          meta: {
            title: '社区活动管理',
          },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('@/views/admin/AdminOperationLogsView.vue'),
          meta: {
            title: '操作日志',
          },
        },
        // 兼容旧路径
        {
          path: 'care',
          redirect: '/admin/services',
        },
      ],
    },
  ],
})

function getRequiredRoles(to) {
  for (let i = to.matched.length - 1; i >= 0; i -= 1) {
    const roles = to.matched[i].meta?.roles
    if (roles && roles.length) return roles
  }
  return null
}

const APP_NAME = '智慧养老服务平台'

function resolveDocumentTitle(to) {
  // 管理端首页标签统一为「管理端」，面包屑仍用 meta.title「工作台」
  if (to.path === '/admin/dashboard' || to.name === 'admin-dashboard') {
    return `${APP_NAME} - 管理端`
  }
  if (to.path === '/staff' || to.path === '/staff/' || to.name === 'staff-home') {
    return `${APP_NAME} - 护理员中心`
  }
  if (to.path === '/family' || to.path === '/family/' || to.name === 'family-home') {
    return `${APP_NAME} - 家属中心`
  }

  const pageTitle = [...to.matched]
    .reverse()
    .find((r) => r.meta?.title)?.meta?.title

  if (to.path.startsWith('/admin')) {
    return pageTitle ? `${APP_NAME} - ${pageTitle}` : `${APP_NAME} - 管理端`
  }
  if (to.path.startsWith('/family')) {
    return pageTitle ? `${APP_NAME} - ${pageTitle}` : `${APP_NAME} - 家属中心`
  }
  if (to.path.startsWith('/staff')) {
    return pageTitle ? `${APP_NAME} - ${pageTitle}` : `${APP_NAME} - 护理员中心`
  }
  if (to.path === '/login') {
    return APP_NAME
  }
  if (pageTitle) {
    return `${APP_NAME} - ${pageTitle}`
  }
  return APP_NAME
}

router.afterEach((to) => {
  document.title = resolveDocumentTitle(to)
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  // 每次导航先与 localStorage/JWT 对齐，消除多标签登录造成的角色撕裂
  userStore.hydrateFromStorage()
  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
  const loggedIn = Boolean(userStore.token)
  const mustChange = Boolean(userStore.mustChangePassword)

  // 首次登录强制改密：拦截一切业务页（含直接 URL / 浏览器后退）
  if (loggedIn && mustChange) {
    if (to.path === '/change-password') {
      return true
    }
    return { path: '/change-password', replace: true }
  }

  if (to.path === '/change-password') {
    if (!loggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 已登录均可访问（首次强制 / 主动改密）；mustChange 拦截已在上方处理业务页
    return true
  }

  if (to.path === '/login') {
    if (loggedIn) {
      const home = userStore.getHomePath()
      // 无有效角色时 getHomePath 会落到 /login，禁止再 redirect 自身（否则无限重定向白屏）
      if (home && home !== '/login') {
        return home
      }
      userStore.resetState()
    }
    return true
  }

  if (requiresAuth && !loggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  const requiredRoles = getRequiredRoles(to)
  if (requiredRoles && !hasAnyRole(userStore.roles, requiredRoles)) {
    const home = userStore.getHomePath()
    if (home && home !== to.path) {
      return home === '/login'
        ? { path: '/login', query: { redirect: to.fullPath } }
        : home
    }
    userStore.resetState()
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  return true
})

export default router

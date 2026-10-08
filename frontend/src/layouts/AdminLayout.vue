<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  Clock,
  Document,
  FirstAidKit,
  Fold,
  Expand,
  House,
  Monitor,
  Notebook,
  Setting,
  Star,
  User,
  UserFilled,
  Warning,
  Dish,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UserAccountMenu from '@/components/UserAccountMenu.vue'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const isNarrow = ref(false)

const menuGroups = [
  {
    key: 'home',
    items: [
      { path: '/admin/dashboard', title: '工作台', icon: House, exact: true },
      { path: '/admin/statistics', title: '运营统计', icon: Monitor },
    ],
  },
  {
    key: 'biz',
    label: '业务管理',
    items: [
      { path: '/admin/elders', title: '老人管理', icon: User },
      { path: '/admin/staff', title: '护理员管理', icon: UserFilled },
      { path: '/admin/users', title: '用户管理', icon: Setting },
    ],
  },
  {
    key: 'health',
    label: '健康管理',
    items: [
      { path: '/admin/health', title: '健康档案', icon: Monitor },
      { path: '/admin/warnings', title: '异常预警', icon: Warning },
    ],
  },
  {
    key: 'care',
    label: '照护管理',
    items: [
      { path: '/admin/services', title: '服务项目', icon: FirstAidKit },
      { path: '/admin/orders', title: '服务订单', icon: Document },
      { path: '/admin/schedules', title: '排班管理', icon: Calendar },
      { path: '/admin/evaluations', title: '服务评价管理', icon: Star },
      { path: '/admin/leaves', title: '护理员请假审批', icon: Bell },
      { path: '/admin/attendance', title: '护理员考勤', icon: Clock },
      { path: '/admin/medications', title: '老人用药', icon: FirstAidKit },
      { path: '/admin/menus', title: '每周膳食菜单', icon: Dish },
      { path: '/admin/elder-diet', title: '老人饮食管理', icon: Dish },
      { path: '/admin/activities', title: '社区活动管理', icon: Calendar },
    ],
  },
  {
    key: 'system',
    label: '系统管理',
    items: [{ path: '/admin/logs', title: '操作日志', icon: Notebook }],
  },
]

const breadcrumbTitle = computed(() => route.meta?.title || '工作台')

function isActive(item) {
  if (item.exact) return route.path === item.path
  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}

function go(path) {
  router.push(path)
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function onNotify() {
  ElMessage.info('通知功能暂未开放')
}

function syncNarrow() {
  isNarrow.value = window.innerWidth < 960
  if (isNarrow.value) collapsed.value = true
}

onMounted(() => {
  syncNarrow()
  window.addEventListener('resize', syncNarrow)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncNarrow)
})
</script>

<template>
  <div class="admin-layout" :class="{ 'is-collapsed': collapsed }">
    <aside class="admin-aside">
      <div class="admin-aside__brand" @click="go('/admin/dashboard')">
        <span class="admin-aside__logo" aria-hidden="true">护</span>
        <div v-if="!collapsed" class="admin-aside__titles">
          <strong>智慧养老服务平台</strong>
          <span>Elder Care Management System</span>
        </div>
      </div>

      <nav class="admin-aside__nav" aria-label="管理端导航">
        <template v-for="group in menuGroups" :key="group.key">
          <div v-if="group.label && !collapsed" class="admin-aside__group">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.path"
            type="button"
            class="admin-aside__item"
            :class="{ 'is-active': isActive(item) }"
            :title="collapsed ? item.title : undefined"
            @click="go(item.path)"
          >
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
            <span v-if="!collapsed">{{ item.title }}</span>
          </button>
        </template>
      </nav>

      <div v-if="!collapsed" class="admin-aside__foot" aria-hidden="true">
        <div class="admin-aside__scene">
          <span class="leaf leaf-a" />
          <span class="leaf leaf-b" />
          <span class="person person-a" />
          <span class="person person-b" />
        </div>
        <p>关爱老人 · 从心开始</p>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="admin-header__left">
          <button type="button" class="admin-header__fold" @click="toggleCollapse">
            <el-icon :size="18">
              <Fold v-if="!collapsed" />
              <Expand v-else />
            </el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理端</el-breadcrumb-item>
            <el-breadcrumb-item>
              <span class="admin-header__crumb-current">{{ breadcrumbTitle }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header__right">
          <button type="button" class="admin-header__bell" title="通知" @click="onNotify">
            <el-icon :size="18"><Bell /></el-icon>
          </button>
          <UserAccountMenu role-hint="管理员" show-avatar profile-path="/admin/profile" />
        </div>
      </header>

      <div class="admin-main__body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  --aside-w: 236px;
  display: flex;
  min-height: 100%;
  height: 100%;
  background: var(--ec-bg-page-grad);
}

.admin-layout.is-collapsed {
  --aside-w: 72px;
}

.admin-aside {
  width: var(--aside-w);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--ec-aside-grad);
  color: #edf8fb;
  transition: width 0.2s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 4px 0 24px rgba(47, 100, 130, 0.12);
}

.admin-aside__brand {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  cursor: pointer;
  flex-shrink: 0;
}

.admin-aside__logo {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--ec-color-primary-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.admin-aside__titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-aside__titles strong {
  font-size: 14px;
  line-height: 1.3;
  white-space: nowrap;
  color: #fff;
}

.admin-aside__titles span {
  font-size: 10px;
  opacity: 0.78;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.admin-aside__nav {
  flex: 1;
  overflow: auto;
  padding: 14px 12px 8px;
}

.admin-aside__group {
  margin: 14px 10px 8px;
  font-size: 11px;
  letter-spacing: 0.06em;
  opacity: 0.62;
  color: #e8f6f8;
}

.admin-aside__item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.88);
  border-radius: 12px;
  padding: 11px 12px;
  margin-bottom: 4px;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.admin-aside__item:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.admin-aside__item.is-active {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  box-shadow: 0 4px 14px rgba(20, 60, 80, 0.12);
  font-weight: 600;
}

.admin-aside__foot {
  flex-shrink: 0;
  padding: 10px 14px 18px;
  text-align: center;
}

.admin-aside__scene {
  position: relative;
  height: 72px;
  margin-bottom: 6px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.04) 100%);
  overflow: hidden;
}

.leaf {
  position: absolute;
  width: 18px;
  height: 28px;
  border-radius: 60% 40% 60% 40%;
  background: rgba(180, 230, 200, 0.45);
  transform: rotate(-25deg);
}

.leaf-a {
  left: 14px;
  bottom: 10px;
}

.leaf-b {
  right: 16px;
  bottom: 18px;
  width: 14px;
  height: 22px;
  background: rgba(160, 220, 190, 0.4);
  transform: rotate(20deg);
}

.person {
  position: absolute;
  bottom: 12px;
  width: 18px;
  height: 34px;
  border-radius: 10px 10px 6px 6px;
  background: rgba(255, 255, 255, 0.35);
}

.person::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  width: 12px;
  height: 12px;
  margin-left: -6px;
  border-radius: 50%;
  background: inherit;
}

.person-a {
  left: 42%;
}

.person-b {
  left: 54%;
  height: 30px;
  width: 16px;
  background: rgba(255, 255, 255, 0.28);
}

.admin-aside__foot p {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
  letter-spacing: 0.04em;
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid rgba(63, 143, 196, 0.08);
  box-shadow: 0 2px 12px rgba(80, 120, 150, 0.04);
  flex-shrink: 0;
  z-index: 2;
}

.admin-header__left,
.admin-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-header__fold,
.admin-header__bell {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: rgba(63, 143, 196, 0.08);
  color: var(--ec-color-primary-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s ease;
}

.admin-header__fold:hover,
.admin-header__bell:hover {
  background: rgba(63, 143, 196, 0.16);
}

.admin-header__crumb-current {
  color: var(--ec-text);
  font-weight: 600;
}

.admin-main__body {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background:
    radial-gradient(ellipse 80% 50% at 100% 0%, rgba(86, 180, 154, 0.06), transparent 55%),
    radial-gradient(ellipse 60% 40% at 0% 100%, rgba(63, 143, 196, 0.05), transparent 50%),
    transparent;
}

@media (max-width: 960px) {
  .admin-main__body {
    padding: 14px;
  }

  .admin-aside__foot {
    display: none;
  }
}
</style>

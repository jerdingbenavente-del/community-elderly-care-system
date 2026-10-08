<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  Clock,
  Document,
  Dish,
  Expand,
  Fold,
  House,
  Notebook,
  User,
  Warning,
} from '@element-plus/icons-vue'
import UserAccountMenu from '@/components/UserAccountMenu.vue'
import { useUserStore } from '@/stores/user'
import { staffFooterDecor, staffPageBackground, staffSidebarPlant } from '@/config/staffImages'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const isNarrow = ref(false)

const menus = [
  { path: '/staff', title: '工作台', icon: House, exact: true },
  { path: '/staff/services', title: '我的服务', icon: Document },
  { path: '/staff/service-records', title: '服务记录', icon: Notebook },
  { path: '/staff/schedules', title: '我的排班', icon: Calendar },
  { path: '/staff/leaves', title: '临时请假', icon: Warning },
  { path: '/staff/attendance', title: '我的考勤', icon: Clock },
  { path: '/staff/menu', title: '今日膳食', icon: Dish },
  { path: '/staff/activities', title: '社区活动', icon: Calendar },
  { path: '/staff/profile', title: '个人中心', icon: User },
]

const pageTitle = computed(() => route.meta?.title || '工作台')
const staffName = computed(() => userStore.displayName)

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

function syncNarrow() {
  isNarrow.value = window.innerWidth < 960
  if (isNarrow.value) collapsed.value = true
}

function onNotify() {
  router.push('/staff/services')
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
  <div
    class="staff-shell"
    :class="{ 'is-collapsed': collapsed }"
    :style="{
      '--page-bg-img': `url(${staffPageBackground})`,
      '--sidebar-plant': `url(${staffSidebarPlant})`,
      '--footer-deco': `url(${staffFooterDecor})`,
    }"
  >
    <div class="staff-shell__wash" aria-hidden="true" />
    <div class="staff-shell__plant" aria-hidden="true" />

    <aside class="staff-sidebar">
      <div class="staff-sidebar__brand" @click="go('/staff')">
        <span class="staff-sidebar__logo" aria-hidden="true">护</span>
        <div v-if="!collapsed" class="staff-sidebar__titles">
          <strong>智慧养老服务平台</strong>
          <span>Elder Care Management System</span>
          <em>护理员工作台</em>
        </div>
      </div>

      <nav class="staff-sidebar__nav" aria-label="护理员端导航">
        <button
          v-for="item in menus"
          :key="item.path"
          type="button"
          class="staff-side-item"
          :class="{ 'is-active': isActive(item) }"
          :title="collapsed ? item.title : undefined"
          @click="go(item.path)"
        >
          <span class="staff-side-item__icon">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </span>
          <span v-if="!collapsed">{{ item.title }}</span>
        </button>
      </nav>

      <div v-if="!collapsed" class="staff-sidebar__identity">
        <strong>专业护理中心</strong>
        <p>
          <template v-if="staffName">{{ staffName }} · </template>
          用专业守护每一位老人
        </p>
      </div>

      <div v-if="!collapsed" class="staff-sidebar__foot" aria-hidden="true">
        <div class="staff-sidebar__scene">
          <span class="deco deco-leaf deco-leaf--a" />
          <span class="deco deco-leaf deco-leaf--b" />
          <span class="deco deco-flower" />
          <span class="deco deco-person deco-person--a" />
          <span class="deco deco-person deco-person--b" />
        </div>
        <p>用专业守护 · 每一份健康</p>
      </div>
    </aside>

    <div class="staff-main">
      <header class="staff-header">
        <div class="staff-header__left">
          <button type="button" class="staff-header__fold" @click="toggleCollapse">
            <el-icon :size="18">
              <Fold v-if="!collapsed" />
              <Expand v-else />
            </el-icon>
          </button>
          <span class="staff-header__badge">护理员工作台</span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/staff' }">护理员端</el-breadcrumb-item>
            <el-breadcrumb-item>
              <span class="staff-header__crumb">{{ pageTitle }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="staff-header__right">
          <button type="button" class="staff-header__bell" title="我的服务" @click="onNotify">
            <el-icon :size="18"><Bell /></el-icon>
          </button>
          <UserAccountMenu role-hint="护理员" show-avatar profile-path="/staff/profile" />
        </div>
      </header>

      <div class="staff-main__body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.staff-shell {
  --aside-w: 236px;
  --staff-blue: #3f9eb9;
  --staff-green: #58b79b;
  --staff-text: #244a65;
  --staff-icon: #5e8aa2;
  min-height: 100%;
  height: 100%;
  display: flex;
  position: relative;
  background: linear-gradient(135deg, #eaf4fb 0%, #eef7f3 50%, #e7f1fa 100%);
  color: var(--staff-text);
  overflow: hidden;
}

.staff-shell.is-collapsed {
  --aside-w: 78px;
}

.staff-shell__wash {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: var(--page-bg-img);
  background-size: cover;
  background-position: center bottom;
  opacity: 0.12;
}

.staff-shell__wash::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(234, 244, 251, 0.9) 0%,
    rgba(238, 247, 243, 0.88) 50%,
    rgba(231, 241, 250, 0.92) 100%
  );
}

.staff-shell__plant {
  position: fixed;
  right: 0;
  bottom: 0;
  z-index: 0;
  width: min(380px, 32vw);
  height: 150px;
  pointer-events: none;
  background-image: var(--footer-deco);
  background-size: cover;
  background-position: right bottom;
  opacity: 0.16;
  mask-image: linear-gradient(to top left, #000 12%, transparent 78%);
}

.staff-sidebar {
  position: relative;
  z-index: 2;
  width: var(--aside-w);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 12px 12px;
  background: linear-gradient(180deg, #eaf7fc 0%, #f3faf8 55%, #eef9f3 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 6px 0 28px rgba(80, 130, 160, 0.06);
  transition: width 0.18s ease;
  overflow: hidden;
}

.staff-sidebar::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140px;
  background-image: var(--sidebar-plant);
  background-size: cover;
  background-position: center bottom;
  opacity: 0.14;
  pointer-events: none;
  mask-image: linear-gradient(to top, #000 25%, transparent 100%);
}

.staff-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 2px 14px;
  padding: 12px 10px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(223, 243, 234, 0.45));
  border: 1px solid rgba(255, 255, 255, 0.85);
  cursor: pointer;
  position: relative;
  z-index: 1;
}

.staff-sidebar__logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--staff-blue), var(--staff-green));
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 17px;
  box-shadow: 0 6px 14px rgba(63, 158, 185, 0.22);
  flex-shrink: 0;
}

.staff-sidebar__titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.staff-sidebar__titles strong {
  font-size: 13px;
  color: var(--staff-text);
  font-weight: 700;
  white-space: nowrap;
}

.staff-sidebar__titles span {
  margin-top: 2px;
  font-size: 10px;
  color: #8aa0b5;
  white-space: nowrap;
}

.staff-sidebar__titles em {
  margin-top: 4px;
  font-style: normal;
  font-size: 11px;
  color: var(--staff-blue);
  font-weight: 600;
}

.staff-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 2px;
  position: relative;
  z-index: 1;
}

.staff-side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 46px;
  border: none;
  background: transparent;
  color: var(--staff-text);
  font-size: 14px;
  padding: 0 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.staff-side-item__icon {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--staff-icon);
  background: rgba(255, 255, 255, 0.55);
  flex-shrink: 0;
}

.staff-side-item:hover {
  color: var(--staff-blue);
  background: rgba(255, 255, 255, 0.72);
}

.staff-side-item.is-active {
  background: #fff;
  color: var(--staff-blue);
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(63, 120, 150, 0.1);
}

.staff-side-item.is-active .staff-side-item__icon {
  color: var(--staff-blue);
  background: rgba(63, 158, 185, 0.12);
}

.staff-sidebar__identity {
  margin: 10px 4px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.9);
  position: relative;
  z-index: 1;
}

.staff-sidebar__identity strong {
  display: block;
  font-size: 13px;
  color: var(--staff-text);
}

.staff-sidebar__identity p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #6b8499;
}

.staff-sidebar__foot {
  flex-shrink: 0;
  padding: 8px 8px 6px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.staff-sidebar__scene {
  position: relative;
  height: 56px;
  margin-bottom: 4px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(63, 158, 185, 0.08), rgba(88, 183, 155, 0.06));
  overflow: hidden;
}

.deco {
  position: absolute;
  pointer-events: none;
}

.deco-leaf {
  width: 16px;
  height: 24px;
  border-radius: 60% 40% 60% 40%;
  background: rgba(88, 183, 155, 0.35);
  transform: rotate(-25deg);
}

.deco-leaf--a {
  left: 12px;
  bottom: 8px;
}

.deco-leaf--b {
  right: 14px;
  bottom: 14px;
  width: 12px;
  height: 18px;
  transform: rotate(20deg);
}

.deco-flower {
  left: 50%;
  bottom: 22px;
  width: 8px;
  height: 8px;
  margin-left: -4px;
  border-radius: 50%;
  background: rgba(255, 180, 140, 0.5);
}

.deco-person {
  bottom: 8px;
  width: 14px;
  height: 26px;
  border-radius: 8px 8px 4px 4px;
  background: rgba(63, 158, 185, 0.28);
}

.deco-person::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  width: 10px;
  height: 10px;
  margin-left: -5px;
  border-radius: 50%;
  background: inherit;
}

.deco-person--a {
  left: 40%;
}

.deco-person--b {
  left: 54%;
  height: 22px;
  width: 12px;
}

.staff-sidebar__foot p {
  margin: 0;
  font-size: 11px;
  color: #6b8499;
  letter-spacing: 0.04em;
}

.staff-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.staff-header {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 4px 16px rgba(63, 120, 150, 0.05);
  backdrop-filter: blur(10px);
}

.staff-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.staff-header__fold {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: rgba(63, 158, 185, 0.1);
  color: var(--staff-blue);
  cursor: pointer;
  display: grid;
  place-items: center;
}

.staff-header__badge {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(63, 158, 185, 0.1);
  color: var(--staff-blue);
  font-size: 12px;
  font-weight: 600;
}

.staff-header__crumb {
  color: var(--staff-text);
  font-weight: 600;
}

.staff-header__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.staff-header__bell {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(63, 158, 185, 0.08);
  color: var(--staff-blue);
  cursor: pointer;
  display: grid;
  place-items: center;
}

.staff-main__body {
  flex: 1;
  overflow: auto;
  padding: 18px 20px 28px;
}

@media (max-width: 960px) {
  .staff-header__badge {
    display: none;
  }

  .staff-main__body {
    padding: 14px;
  }
}
</style>

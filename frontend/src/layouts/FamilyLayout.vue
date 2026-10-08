<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  House,
  User,
  UserFilled,
  Monitor,
  FirstAidKit,
  Document,
  Star,
  Dish,
  Calendar,
} from '@element-plus/icons-vue'
import UserAccountMenu from '@/components/UserAccountMenu.vue'
import { useUserStore } from '@/stores/user'
import {
  pageBackgroundImage,
  sidebarPlantImage,
  cornerPlantLeft,
  cornerPlantRight,
} from '@/config/familyImages'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/family', title: '首页', exact: true, icon: House },
  { path: '/family/elders', title: '我的老人', icon: User },
  { path: '/family/health', title: '健康管理', icon: Monitor },
  { path: '/family/care-services', title: '照护服务', icon: FirstAidKit },
  { path: '/family/orders', title: '我的订单', icon: Document },
  { path: '/family/evaluations', title: '服务评价', icon: Star },
  { path: '/family/menu', title: '膳食菜单', icon: Dish },
  { path: '/family/activities', title: '社区活动', icon: Calendar },
  { path: '/family/profile', title: '我的', icon: UserFilled },
]

const familyName = computed(() => userStore.displayName)

function isActive(item) {
  if (item.exact) return route.path === item.path
  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}

function go(path) {
  router.push(path)
}
</script>

<template>
  <div
    class="family-shell"
    :style="{
      '--page-bg-img': `url(${pageBackgroundImage})`,
      '--plant-left': `url(${cornerPlantLeft})`,
      '--plant-right': `url(${cornerPlantRight})`,
      '--sidebar-plant': `url(${sidebarPlantImage})`,
    }"
  >
    <div class="family-shell__wash" aria-hidden="true" />
    <div class="family-shell__plant family-shell__plant--left" aria-hidden="true" />
    <div class="family-shell__plant family-shell__plant--right" aria-hidden="true" />

    <aside class="family-sidebar">
      <div class="family-sidebar__brand" @click="go('/family')">
        <span class="family-sidebar__logo" aria-hidden="true">养</span>
        <div class="family-sidebar__titles">
          <strong>智慧养老服务平台</strong>
          <span>Elder Care Management System</span>
          <em>家属服务中心</em>
        </div>
      </div>

      <nav class="family-sidebar__nav" aria-label="家属端导航">
        <button
          v-for="item in menus"
          :key="item.path"
          type="button"
          class="family-side-item"
          :class="{ 'is-active': isActive(item) }"
          @click="go(item.path)"
        >
          <span class="family-side-item__icon">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </span>
          <span>{{ item.title }}</span>
        </button>
      </nav>

      <div class="family-sidebar__identity">
        <strong>家庭关爱中心</strong>
        <p>
          <template v-if="familyName">{{ familyName }} · </template>
          守护家人的健康与晚年生活
        </p>
      </div>

      <div class="family-sidebar__foot" aria-hidden="true">
        <div class="family-sidebar__scene">
          <span class="deco deco-leaf deco-leaf--a" />
          <span class="deco deco-leaf deco-leaf--b" />
          <span class="deco deco-flower" />
          <span class="deco deco-person deco-person--a" />
          <span class="deco deco-person deco-person--b" />
        </div>
        <p>关爱老人 · 从心开始</p>
      </div>
    </aside>

    <div class="family-main">
      <header class="family-header">
        <div class="family-header__left">
          <span class="family-header__badge">家属服务中心</span>
        </div>
        <div class="family-header__right">
          <button
            type="button"
            class="family-header__bell"
            title="健康预警"
            @click="go('/family/health')"
          >
            <el-icon :size="18"><Bell /></el-icon>
          </button>
          <UserAccountMenu show-avatar profile-path="/family/profile" />
        </div>
      </header>

      <div class="family-main__body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.family-shell {
  --page-bg-img: none;
  --plant-left: none;
  --plant-right: none;
  --sidebar-plant: none;
  --fam-blue: #4a9bd1;
  --fam-green: #68c89b;
  --fam-text: #335b73;
  --fam-icon: #5e8aa2;
  min-height: 100vh;
  display: flex;
  position: relative;
  background: linear-gradient(135deg, #eaf4fb 0%, #eef7f3 50%, #e7f1fa 100%);
  overflow: hidden;
  color: var(--ec-text);
}

.family-shell__wash {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: var(--page-bg-img);
  background-size: cover;
  background-position: center bottom;
  opacity: 0.14;
}

.family-shell__wash::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(234, 244, 251, 0.88) 0%,
    rgba(238, 247, 243, 0.86) 50%,
    rgba(231, 241, 250, 0.9) 100%
  );
}

.family-shell__plant {
  position: fixed;
  z-index: 0;
  pointer-events: none;
  background-size: cover;
  background-repeat: no-repeat;
  opacity: 0.28;
}

.family-shell__plant--left {
  left: 200px;
  bottom: -20px;
  width: 280px;
  height: 180px;
  background-image: var(--plant-left);
  background-position: left bottom;
  mask-image: linear-gradient(to top right, #000 20%, transparent 85%);
}

.family-shell__plant--right {
  right: -30px;
  bottom: -30px;
  width: 340px;
  height: 220px;
  background-image: var(--plant-right);
  background-position: right bottom;
  mask-image: linear-gradient(to top left, #000 15%, transparent 80%);
}

/* —— Sidebar：浅蓝 + 浅绿，温馨家庭风 —— */
.family-sidebar {
  position: relative;
  z-index: 20;
  width: var(--ec-sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 12px 12px;
  background: linear-gradient(180deg, #eaf7fc 0%, #f3faf8 55%, #eef9f3 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 6px 0 28px rgba(80, 130, 160, 0.06);
}

.family-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 2px 14px;
  padding: 12px 10px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(223, 243, 234, 0.45));
  border: 1px solid rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: background 0.15s ease;
}

.family-sidebar__brand:hover {
  background: rgba(255, 255, 255, 0.85);
}

.family-sidebar__logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--fam-blue), var(--fam-green));
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 17px;
  box-shadow: 0 6px 14px rgba(74, 155, 209, 0.22);
  flex-shrink: 0;
}

.family-sidebar__titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.family-sidebar__titles strong {
  font-size: 13px;
  color: var(--fam-text);
  font-weight: 700;
}

.family-sidebar__titles span {
  margin-top: 2px;
  font-size: 10px;
  color: #8aa0b5;
  letter-spacing: 0.02em;
}

.family-sidebar__titles em {
  margin-top: 4px;
  font-style: normal;
  font-size: 11px;
  color: var(--fam-blue);
  font-weight: 600;
}

.family-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 2px;
}

.family-side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 46px;
  border: none;
  background: transparent;
  color: var(--fam-text);
  font-size: 14px;
  padding: 0 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.family-side-item__icon {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--fam-icon);
  background: rgba(255, 255, 255, 0.55);
  flex-shrink: 0;
  transition: background 0.18s ease, color 0.18s ease;
}

.family-side-item:hover {
  color: var(--fam-blue);
  background: rgba(255, 255, 255, 0.65);
}

.family-side-item:hover .family-side-item__icon {
  color: var(--fam-blue);
  background: rgba(74, 155, 209, 0.12);
}

.family-side-item.is-active {
  color: #fff;
  background: linear-gradient(90deg, #63b0de, #72cba0);
  box-shadow: 0 5px 16px rgba(74, 155, 209, 0.22);
  font-weight: 600;
}

.family-side-item.is-active .family-side-item__icon {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

.family-sidebar__identity {
  margin: 10px 2px 8px;
  padding: 12px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 12px rgba(80, 130, 150, 0.04);
}

.family-sidebar__identity strong {
  display: block;
  font-size: 13px;
  color: var(--fam-text);
  margin-bottom: 4px;
}

.family-sidebar__identity p {
  margin: 0;
  font-size: 11px;
  line-height: 1.45;
  color: #6b8a9e;
}

.family-sidebar__foot {
  margin: 0 2px;
  padding: 10px 10px 12px;
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(234, 247, 252, 0.35), rgba(223, 243, 234, 0.55)),
    var(--sidebar-plant);
  background-size: cover;
  background-position: center bottom;
  border: 1px solid rgba(255, 255, 255, 0.65);
  text-align: center;
  overflow: hidden;
}

.family-sidebar__scene {
  position: relative;
  height: 56px;
  margin-bottom: 4px;
}

.deco {
  position: absolute;
  pointer-events: none;
}

.deco-leaf {
  width: 16px;
  height: 24px;
  border-radius: 60% 40% 55% 45%;
  background: rgba(104, 200, 155, 0.45);
  bottom: 8px;
}

.deco-leaf--a {
  left: 18%;
  transform: rotate(-28deg);
}

.deco-leaf--b {
  right: 20%;
  width: 12px;
  height: 18px;
  background: rgba(74, 155, 209, 0.28);
  transform: rotate(22deg);
}

.deco-flower {
  left: 48%;
  bottom: 10px;
  width: 10px;
  height: 10px;
  margin-left: -5px;
  border-radius: 50%;
  background: rgba(240, 170, 120, 0.55);
  box-shadow:
    0 -6px 0 -2px rgba(240, 170, 120, 0.4),
    6px 0 0 -2px rgba(240, 170, 120, 0.35),
    0 6px 0 -2px rgba(240, 170, 120, 0.35),
    -6px 0 0 -2px rgba(240, 170, 120, 0.35);
}

.deco-person {
  bottom: 6px;
  width: 14px;
  height: 26px;
  border-radius: 8px 8px 5px 5px;
  background: rgba(255, 255, 255, 0.55);
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
  left: 38%;
}

.deco-person--b {
  left: 52%;
  height: 22px;
  width: 12px;
  background: rgba(255, 255, 255, 0.42);
}

.family-sidebar__foot p {
  margin: 0;
  font-size: 12px;
  font-weight: 650;
  color: #3a6f8f;
  letter-spacing: 0.04em;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.7);
}

.family-main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.family-header {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 18px rgba(80, 120, 150, 0.05);
}

.family-header__badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(74, 155, 209, 0.1);
  color: var(--fam-blue);
  font-size: 12px;
  font-weight: 600;
}

.family-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.family-header__bell {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(74, 155, 209, 0.14);
  background: rgba(255, 255, 255, 0.75);
  color: #718096;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.family-header__bell:hover {
  color: var(--fam-blue);
}

.family-main__body {
  flex: 1;
  padding: 18px 22px 28px;
  overflow: auto;
}

@media (max-width: 1100px) {
  .family-shell {
    flex-direction: column;
  }

  .family-sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
  }

  .family-sidebar__brand {
    margin: 0;
    padding: 8px 10px;
    flex: 0 0 auto;
  }

  .family-sidebar__titles em {
    display: none;
  }

  .family-sidebar__nav {
    flex-direction: row;
    overflow-x: auto;
    flex: 1;
    min-width: 0;
  }

  .family-side-item {
    min-height: 40px;
    white-space: nowrap;
  }

  .family-sidebar__identity,
  .family-sidebar__foot,
  .family-shell__plant {
    display: none;
  }

  .family-main__body {
    padding: 14px 12px 20px;
  }
}
</style>

<script setup>
defineProps({
  menus: {
    type: Array,
    default: () => [],
  },
  collapse: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <aside class="app-sidebar" :class="{ 'is-collapse': collapse }">
    <div class="app-sidebar__brand">
      <span class="app-sidebar__logo">护</span>
      <span v-if="!collapse" class="app-sidebar__text">智慧养老</span>
    </div>
    <el-menu
      :default-active="$route.path"
      :collapse="collapse"
      router
      class="app-sidebar__menu"
      background-color="#2c5f7c"
      text-color="#dceaf3"
      active-text-color="#ffffff"
    >
      <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
        <span>{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<style scoped>
.app-sidebar {
  width: var(--ec-sidebar-width);
  flex-shrink: 0;
  background: var(--ec-color-primary-dark);
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.app-sidebar.is-collapse {
  width: 64px;
}

.app-sidebar__brand {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-sidebar__logo {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--ec-color-secondary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.app-sidebar__text {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
}

.app-sidebar__menu {
  border-right: none;
  flex: 1;
}

.app-sidebar__menu:not(.el-menu--collapse) {
  width: var(--ec-sidebar-width);
}
</style>

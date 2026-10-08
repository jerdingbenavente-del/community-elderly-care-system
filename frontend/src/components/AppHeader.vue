<script setup>
defineProps({
  title: {
    type: String,
    default: '智慧养老服务平台',
  },
  showMenu: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <span class="app-header__logo">护</span>
      <span class="app-header__title">{{ title }}</span>
    </div>
    <div class="app-header__center">
      <slot name="nav" />
    </div>
    <div class="app-header__right">
      <slot name="right">
        <span class="app-header__user">未登录</span>
      </slot>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: var(--ec-header-height);
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: var(--ec-bg-card);
  border-bottom: 1px solid var(--ec-border);
  box-shadow: var(--ec-shadow);
}

.app-header__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.app-header__logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--ec-color-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.app-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ec-color-primary-dark);
  white-space: nowrap;
}

.app-header__center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
  margin: 0 16px;
}

.app-header__right {
  flex-shrink: 0;
}

.app-header__user {
  color: var(--ec-text-secondary);
  font-size: 13px;
}
</style>

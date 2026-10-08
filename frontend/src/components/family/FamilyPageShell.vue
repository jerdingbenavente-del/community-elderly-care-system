<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="family-page">
    <header class="family-page__head">
      <div>
        <h2 class="family-page__title">{{ title }}</h2>
        <p v-if="subtitle" class="family-page__sub">{{ subtitle }}</p>
      </div>
      <div class="family-page__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="family-page__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.family-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.family-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.family-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c4a5e;
}

.family-page__sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #718096;
}

.family-page__body {
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 16px;
  min-height: 360px;
}
</style>

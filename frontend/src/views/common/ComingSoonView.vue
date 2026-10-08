<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Tools } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
})

const route = useRoute()

const displayTitle = computed(
  () => props.title || route.meta?.title || '功能建设中',
)
const displayDesc = computed(
  () =>
    props.description ||
    route.meta?.description ||
    '该功能正在建设中，将在后续阶段完善。',
)
</script>

<template>
  <div class="coming-soon">
    <div class="coming-soon__card">
      <div class="coming-soon__icon">
        <el-icon :size="36"><Tools /></el-icon>
      </div>
      <h2>{{ displayTitle }}</h2>
      <p>{{ displayDesc }}</p>
      <span class="coming-soon__path">{{ route.path }}</span>
    </div>
  </div>
</template>

<style scoped>
.coming-soon {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.coming-soon__card {
  width: 100%;
  max-width: 520px;
  text-align: center;
  padding: 40px 28px;
  border-radius: var(--ec-radius);
  background: var(--ec-bg-card);
  box-shadow: var(--ec-shadow);
  backdrop-filter: blur(8px);
}

.coming-soon__icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--ec-color-primary), var(--ec-color-secondary));
}

.coming-soon__card h2 {
  margin: 0 0 10px;
  font-size: 22px;
  color: var(--ec-text);
}

.coming-soon__card p {
  margin: 0;
  color: var(--ec-text-secondary);
  line-height: 1.7;
  font-size: 14px;
}

.coming-soon__path {
  display: inline-block;
  margin-top: 16px;
  font-size: 12px;
  color: var(--ec-text-muted);
}
</style>

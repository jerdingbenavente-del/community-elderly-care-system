<script setup>
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  unhandledCount: { type: Number, default: null },
})

const router = useRouter()

function goAlert() {
  router.push('/family/health')
}
</script>

<template>
  <div v-if="loading" class="alert-panel alert-panel--loading">
    <el-skeleton :rows="1" animated />
  </div>
  <el-alert
    v-else-if="error"
    type="error"
    :title="error"
    :closable="false"
    show-icon
  />
  <div
    v-else-if="unhandledCount > 0"
    class="alert-panel alert-panel--warn"
  >
    <div class="alert-panel__main">
      <span class="alert-panel__icon">
        <el-icon :size="18"><Bell /></el-icon>
      </span>
      <div>
        <div class="alert-panel__title">当前有 {{ unhandledCount }} 条健康预警</div>
        <div class="alert-panel__desc">请及时关注老人健康情况，避免风险。</div>
      </div>
    </div>
    <button type="button" class="alert-panel__link" @click="goAlert">
      查看详情 →
    </button>
  </div>
  <div v-else class="alert-panel alert-panel--ok">
    <div class="alert-panel__main">
      <span class="alert-panel__icon">
        <el-icon :size="18"><Bell /></el-icon>
      </span>
      <div>
        <div class="alert-panel__title">暂无异常预警</div>
        <div class="alert-panel__desc">老人健康状况平稳，请继续保持关注。</div>
      </div>
    </div>
    <button type="button" class="alert-panel__link" @click="goAlert">
      查看详情 →
    </button>
  </div>
</template>

<style scoped>
.alert-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-radius: 16px;
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: var(--ec-shadow);
  backdrop-filter: blur(8px);
}

.alert-panel--warn {
  background: linear-gradient(90deg, rgba(255, 244, 232, 0.95), rgba(254, 240, 240, 0.92));
  color: #c8782a;
}

.alert-panel--ok {
  background: linear-gradient(90deg, rgba(234, 246, 239, 0.95), rgba(232, 244, 251, 0.9));
  color: #4f9a72;
}

.alert-panel--loading {
  background: rgba(255, 255, 255, 0.68);
  padding: 10px 14px;
}

.alert-panel__main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.alert-panel__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

.alert-panel--warn .alert-panel__icon {
  color: #f56c6c;
}

.alert-panel__title {
  font-size: 15px;
  font-weight: 700;
}

.alert-panel__desc {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.9;
}

.alert-panel__link {
  border: none;
  background: transparent;
  color: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
</style>

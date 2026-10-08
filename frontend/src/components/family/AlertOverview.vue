<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

const props = defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  unhandledCount: { type: Number, default: null },
})

const router = useRouter()

const tip = computed(() => {
  if (props.unhandledCount == null) return ''
  if (props.unhandledCount <= 0) return '暂无异常预警，家人状态平稳'
  return `当前有 ${props.unhandledCount} 条待处理预警，请及时关注`
})

function goAlert() {
  router.push('/family/health')
}
</script>

<template>
  <section class="panel">
    <div class="panel__head">
      <div>
        <h3 class="panel__title">健康预警</h3>
        <p class="panel__sub">异常指标及时提醒</p>
      </div>
      <el-button link type="primary" @click="goAlert">查看详情</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="2" animated />
    <el-alert v-else-if="error" type="error" :title="error" :closable="false" show-icon />
    <div v-else class="alert-body" :class="{ 'is-warn': unhandledCount > 0 }">
      <div class="alert-body__icon-wrap">
        <el-icon :size="26"><Bell /></el-icon>
      </div>
      <div>
        <div class="alert-body__count">
          <template v-if="unhandledCount > 0">{{ unhandledCount }}</template>
          <template v-else>0</template>
        </div>
        <div class="alert-body__tip">{{ tip }}</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  border: 1px solid var(--ec-border);
  box-shadow: var(--ec-shadow);
  padding: 18px;
  height: 100%;
}

.panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel__title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
}

.panel__sub {
  margin: 0;
  font-size: 12px;
  color: var(--ec-text-muted);
}

.alert-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f0f9eb, #f7fcf4);
  color: #67c23a;
}

.alert-body.is-warn {
  background: linear-gradient(135deg, #fdf6ec, #fff8ee);
  color: #ff9f43;
}

.alert-body__icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-body__count {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}

.alert-body__tip {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ec-text-secondary);
  line-height: 1.5;
}
</style>

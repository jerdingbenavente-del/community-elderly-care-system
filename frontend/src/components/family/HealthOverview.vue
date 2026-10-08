<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Sunny, Odometer, FirstAidKit } from '@element-plus/icons-vue'

const props = defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  record: { type: Object, default: null },
  elderName: { type: String, default: '' },
})

const router = useRouter()

const pressure = computed(() => {
  const r = props.record
  if (!r) return '-'
  if (r.systolicPressure == null && r.diastolicPressure == null) return '-'
  return `${r.systolicPressure ?? '-'}/${r.diastolicPressure ?? '-'}`
})

function goHealth() {
  router.push('/family/health')
}
</script>

<template>
  <section class="info-card">
    <div class="info-card__head">
      <h3>健康概览</h3>
      <button type="button" class="info-card__link" @click="goHealth">查看全部 →</button>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <el-alert v-else-if="error" type="error" :title="error" :closable="false" show-icon />
    <el-empty v-else-if="!record" description="暂无健康记录" :image-size="64" />
    <div v-else class="health">
      <div class="health__metrics">
        <div class="metric metric--temp">
          <span class="metric__icon"><el-icon><Sunny /></el-icon></span>
          <div>
            <div class="metric__label">体温</div>
            <div class="metric__value">{{ record.bodyTemperature ?? '-' }} <small>℃</small></div>
          </div>
        </div>
        <div class="metric metric--bp">
          <span class="metric__icon"><el-icon><FirstAidKit /></el-icon></span>
          <div>
            <div class="metric__label">血压</div>
            <div class="metric__value">{{ pressure }} <small>mmHg</small></div>
          </div>
        </div>
        <div class="metric metric--hr">
          <span class="metric__icon"><el-icon><Odometer /></el-icon></span>
          <div>
            <div class="metric__label">心率</div>
            <div class="metric__value">{{ record.heartRate ?? '-' }} <small>次/分</small></div>
          </div>
        </div>
      </div>
      <p class="health__time">
        <template v-if="elderName">{{ elderName }} · </template>
        测量时间：{{ record.measuredAt || '-' }}
      </p>
    </div>
  </section>
</template>

<style scoped>
.info-card {
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: var(--ec-shadow);
  padding: 16px;
}

.info-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.info-card__head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.info-card__link {
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 12px;
  cursor: pointer;
}

.health__metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
}

.metric--temp {
  background: rgba(254, 240, 240, 0.75);
}
.metric--bp {
  background: rgba(234, 244, 251, 0.8);
}
.metric--hr {
  background: rgba(255, 240, 245, 0.8);
}

.metric__icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
}

.metric--temp .metric__icon {
  color: #f56c6c;
}
.metric--bp .metric__icon {
  color: #4a90c2;
}
.metric--hr .metric__icon {
  color: #e091b0;
}

.metric__label {
  font-size: 12px;
  color: #718096;
}

.metric__value {
  margin-top: 2px;
  font-size: 16px;
  font-weight: 700;
  color: #2c4a5e;
}

.metric__value small {
  font-size: 11px;
  font-weight: 500;
  color: #8aa0b5;
}

.health__time {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8aa0b5;
}
</style>

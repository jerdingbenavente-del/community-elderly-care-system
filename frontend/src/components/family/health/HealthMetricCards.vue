<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/familyHome'

const props = defineProps({
  loading: Boolean,
  record: { type: Object, default: null },
})

const pressure = computed(() => {
  const r = props.record
  if (!r) return '-'
  if (r.systolicPressure == null && r.diastolicPressure == null) return '-'
  return `${r.systolicPressure ?? '-'}/${r.diastolicPressure ?? '-'}`
})

const measuredAt = computed(() =>
  props.record?.measuredAt ? formatDateTime(props.record.measuredAt) : '',
)

const metrics = computed(() => [
  {
    key: 'temp',
    icon: '🌡',
    label: '体温',
    value: props.record?.bodyTemperature != null ? String(props.record.bodyTemperature) : '-',
    unit: '℃',
    tone: 'temp',
  },
  {
    key: 'bp',
    icon: '🩺',
    label: '血压',
    value: pressure.value,
    unit: 'mmHg',
    tone: 'bp',
  },
  {
    key: 'hr',
    icon: '❤️',
    label: '心率',
    value: props.record?.heartRate != null ? String(props.record.heartRate) : '-',
    unit: '次/分',
    tone: 'hr',
  },
  {
    key: 'glu',
    icon: '🩸',
    label: '血糖',
    value: props.record?.bloodGlucose != null ? String(props.record.bloodGlucose) : '-',
    unit: 'mmol/L',
    tone: 'glu',
  },
])
</script>

<template>
  <section class="overview">
    <div class="overview__head">
      <div>
        <h3>健康概览</h3>
        <p v-if="measuredAt">最近测量：{{ measuredAt }}</p>
        <p v-else>最近测量：暂无健康记录</p>
      </div>
      <span class="overview__tip">展示最新测量值，状态判断以后端预警为准</span>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <el-empty
      v-else-if="!record"
      description="暂无健康数据"
      :image-size="72"
    >
      <template #description>
        <p class="empty-title">暂无健康数据</p>
        <p class="empty-desc">暂时还没有该老人的健康测量记录。</p>
      </template>
    </el-empty>
    <div v-else class="overview__grid">
      <article
        v-for="item in metrics"
        :key="item.key"
        class="metric-card"
        :class="`metric-card--${item.tone}`"
      >
        <div class="metric-card__label">
          <span>{{ item.icon }}</span>
          {{ item.label }}
        </div>
        <div class="metric-card__value">
          {{ item.value }}
          <small>{{ item.unit }}</small>
        </div>
        <div class="metric-card__sub">最新测量</div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.overview {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.overview__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.overview__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.overview__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.overview__tip {
  font-size: 12px;
  color: #8aa0b5;
  white-space: nowrap;
}

.overview__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border-radius: 14px;
  padding: 16px;
  min-height: 118px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-card--temp {
  background: linear-gradient(160deg, rgba(255, 232, 232, 0.9), rgba(255, 248, 248, 0.95));
}
.metric-card--bp {
  background: linear-gradient(160deg, rgba(222, 239, 252, 0.95), rgba(245, 250, 255, 0.95));
}
.metric-card--hr {
  background: linear-gradient(160deg, rgba(255, 232, 240, 0.92), rgba(255, 248, 250, 0.95));
}
.metric-card--glu {
  background: linear-gradient(160deg, rgba(228, 244, 236, 0.95), rgba(245, 252, 248, 0.95));
}

.metric-card__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4a6072;
  font-weight: 600;
}

.metric-card__value {
  font-size: 24px;
  font-weight: 700;
  color: #2c4a5e;
  line-height: 1.2;
}

.metric-card__value small {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #8aa0b5;
}

.metric-card__sub {
  margin-top: auto;
  font-size: 12px;
  color: #8aa0b5;
}

.empty-title {
  margin: 0;
  color: #4a6072;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 6px 0 0;
  color: #8aa0b5;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .overview__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview__tip {
    display: none;
  }
}

@media (max-width: 560px) {
  .overview__grid {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  stats: {
    type: Object,
    default: () => ({
      PENDING: null,
      CONFIRMED: null,
      IN_SERVICE: null,
      COMPLETED: null,
    }),
  },
  empty: Boolean,
})

const router = useRouter()

const items = [
  { key: 'PENDING', label: '待确认', tone: 'orange' },
  { key: 'CONFIRMED', label: '已确认', tone: 'green' },
  { key: 'IN_SERVICE', label: '服务中', tone: 'blue' },
  { key: 'COMPLETED', label: '已完成', tone: 'yellow' },
]

function goOrders() {
  router.push('/family/orders')
}
</script>

<template>
  <section class="info-card">
    <div class="info-card__head">
      <h3>服务概览</h3>
      <button type="button" class="info-card__link" @click="goOrders">查看全部 →</button>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <el-alert v-else-if="error" type="error" :title="error" :closable="false" show-icon />
    <el-empty v-else-if="empty" description="暂无服务订单" :image-size="64" />
    <div v-else class="stats">
      <div
        v-for="item in items"
        :key="item.key"
        class="stat"
        :class="`stat--${item.tone}`"
      >
        <div class="stat__value">
          {{ stats[item.key] == null ? '-' : stats[item.key] }}
        </div>
        <div class="stat__label">{{ item.label }}</div>
      </div>
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

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  text-align: center;
  border-radius: 12px;
  padding: 14px 8px;
}

.stat--orange {
  background: rgba(255, 244, 232, 0.9);
}
.stat--green {
  background: rgba(234, 246, 239, 0.9);
}
.stat--blue {
  background: rgba(234, 244, 251, 0.95);
}
.stat--yellow {
  background: rgba(253, 249, 231, 0.95);
}

.stat__value {
  font-size: 22px;
  font-weight: 700;
  color: #2c4a5e;
}

.stat__label {
  margin-top: 4px;
  font-size: 12px;
  color: #718096;
}
</style>

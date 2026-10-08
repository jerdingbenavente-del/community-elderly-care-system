<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/familyHome'

const props = defineProps({
  /** 兼容 evaluation / item */
  evaluation: { type: Object, default: null },
  item: { type: Object, default: null },
})

const emit = defineEmits(['view', 'detail'])

const data = computed(() => props.evaluation || props.item || {})

const tone = computed(() => {
  const s = Number(data.value.score) || 0
  if (s >= 5) return 'green'
  if (s >= 4) return 'blue'
  if (s >= 3) return 'orange'
  return 'rose'
})

function onOpen() {
  emit('view', data.value)
  emit('detail', data.value)
}
</script>

<template>
  <article class="eval-card" :class="`eval-card--${tone}`" @click="onOpen">
    <div class="eval-card__top">
      <div>
        <h3>{{ data.serviceName || '照护服务' }}</h3>
        <p>订单号 {{ data.orderNo || '-' }} · {{ data.elderName || '-' }}</p>
      </div>
      <el-rate :model-value="data.score || 0" disabled />
    </div>
    <p class="eval-card__content">
      {{ data.content || '（未填写文字评价）' }}
    </p>
    <div class="eval-card__foot">
      <span>{{ formatDateTime(data.createdAt) }}</span>
      <button type="button" class="link" @click.stop="onOpen">查看详情 →</button>
    </div>
  </article>
</template>

<style scoped>
.eval-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 16px 18px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.eval-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(74, 144, 194, 0.12);
}

.eval-card--green {
  background: linear-gradient(165deg, rgba(232, 246, 239, 0.95), rgba(255, 255, 255, 0.88));
}
.eval-card--blue {
  background: linear-gradient(165deg, rgba(234, 244, 251, 0.95), rgba(255, 255, 255, 0.88));
}
.eval-card--orange {
  background: linear-gradient(165deg, rgba(255, 244, 232, 0.95), rgba(255, 255, 255, 0.88));
}
.eval-card--rose {
  background: linear-gradient(165deg, rgba(255, 236, 240, 0.95), rgba(255, 255, 255, 0.88));
}

.eval-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.eval-card__top h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.eval-card__top p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8aa0b5;
}

.eval-card__content {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #4a6072;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.eval-card__foot {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #8aa0b5;
}

.link {
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}
</style>

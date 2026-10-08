<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  FirstAidKit,
  House,
  Sunny,
  CoffeeCup,
  Monitor,
} from '@element-plus/icons-vue'
import { formatPrice, serviceTypeLabel } from '@/utils/familyHome'

const props = defineProps({
  item: { type: Object, required: true },
  toneIndex: { type: Number, default: 0 },
})

const router = useRouter()

const tones = ['blue', 'green', 'purple', 'orange']
const tone = computed(() => tones[props.toneIndex % tones.length])

const icon = computed(() => {
  const type = props.item?.serviceType
  if (type === 'REHAB') return Sunny
  if (type === 'HEALTH') return Monitor
  if (type === 'COMPANION') return CoffeeCup
  if (type === 'DAILY') return House
  return FirstAidKit
})

function goDetail() {
  router.push(`/family/care-services/${props.item.id}`)
}
</script>

<template>
  <article class="svc-card" :class="`svc-card--${tone}`" @click="goDetail">
    <div class="svc-card__icon">
      <el-icon :size="26"><component :is="icon" /></el-icon>
    </div>
    <h3>{{ item.serviceName }}</h3>
    <p class="svc-card__type">{{ serviceTypeLabel(item.serviceType) }}</p>
    <p class="svc-card__desc">{{ item.description || '暂无项目说明' }}</p>
    <div class="svc-card__meta">
      <span>⏱ {{ item.durationMinutes ?? '-' }} 分钟</span>
      <span class="price">{{ formatPrice(item.price) }}</span>
    </div>
    <div class="svc-card__foot">
      <el-tag size="small" type="success" effect="light">可预约</el-tag>
      <button type="button" class="link" @click.stop="goDetail">查看详情 →</button>
    </div>
  </article>
</template>

<style scoped>
.svc-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  min-height: 220px;
}

.svc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(74, 144, 194, 0.14);
}

.svc-card--blue {
  background: linear-gradient(165deg, rgba(234, 244, 251, 0.95), rgba(255, 255, 255, 0.82));
}
.svc-card--green {
  background: linear-gradient(165deg, rgba(232, 246, 239, 0.95), rgba(255, 255, 255, 0.82));
}
.svc-card--purple {
  background: linear-gradient(165deg, rgba(240, 236, 252, 0.95), rgba(255, 255, 255, 0.82));
}
.svc-card--orange {
  background: linear-gradient(165deg, rgba(255, 244, 232, 0.95), rgba(255, 255, 255, 0.82));
}

.svc-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4a90c2, #69b98c);
}

.svc-card--green .svc-card__icon {
  background: linear-gradient(135deg, #69b98c, #8fcea8);
}
.svc-card--purple .svc-card__icon {
  background: linear-gradient(135deg, #9b7ed9, #b9a0e8);
}
.svc-card--orange .svc-card__icon {
  background: linear-gradient(135deg, #f2a65a, #f6c28a);
}

.svc-card h3 {
  margin: 4px 0 0;
  font-size: 17px;
  color: #2c4a5e;
}

.svc-card__type {
  margin: 0;
  font-size: 12px;
  color: #4a90c2;
  font-weight: 600;
}

.svc-card__desc {
  margin: 0;
  flex: 1;
  font-size: 13px;
  line-height: 1.55;
  color: #718096;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.svc-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #4a6072;
  font-weight: 600;
}

.svc-card__meta .price {
  color: #c8782a;
}

.svc-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
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

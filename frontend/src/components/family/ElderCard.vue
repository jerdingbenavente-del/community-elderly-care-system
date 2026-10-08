<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Female, Male, User } from '@element-plus/icons-vue'
import { calcAge, genderText } from '@/utils/familyHome'
import { elderStatusText, maskPhone } from '@/utils/sensitive'

const props = defineProps({
  elder: { type: Object, required: true },
  tone: { type: String, default: 'blue' },
})

const router = useRouter()

const age = computed(() => calcAge(props.elder?.birthDate))
const gender = computed(() => genderText(props.elder?.gender))
const statusLabel = computed(() => elderStatusText(props.elder?.status))
const phoneLabel = computed(() => maskPhone(props.elder?.phone))
const statusOk = computed(() => props.elder?.status === 1)

const avatarIcon = computed(() => {
  if (props.elder?.gender === 1) return Male
  if (props.elder?.gender === 2) return Female
  return User
})

function goDetail() {
  if (!props.elder?.id) return
  router.push(`/family/elders/${props.elder.id}`)
}
</script>

<template>
  <article class="elder-card" :class="`elder-card--${tone}`" @click="goDetail">
    <div class="elder-card__avatar">
      <el-icon :size="32"><component :is="avatarIcon" /></el-icon>
    </div>
    <h3 class="elder-card__name">{{ elder.name || '未命名' }}</h3>
    <p class="elder-card__meta">
      {{ gender }}
      <template v-if="age != null"> · {{ age }}岁</template>
    </p>
    <p class="elder-card__phone">📞 {{ phoneLabel }}</p>
    <span class="elder-card__status" :class="statusOk ? 'is-ok' : 'is-off'">
      {{ statusOk ? '🟢' : '⚪' }} {{ statusLabel }}
    </span>
    <button type="button" class="elder-card__link" @click.stop="goDetail">
      查看档案 →
    </button>
  </article>
</template>

<style scoped>
.elder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 22px 18px 18px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.elder-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(74, 144, 194, 0.14);
}

.elder-card--blue {
  background: linear-gradient(165deg, rgba(234, 244, 251, 0.92), rgba(255, 255, 255, 0.78));
}
.elder-card--green {
  background: linear-gradient(165deg, rgba(232, 246, 239, 0.92), rgba(255, 255, 255, 0.78));
}
.elder-card--orange {
  background: linear-gradient(165deg, rgba(255, 246, 236, 0.92), rgba(255, 255, 255, 0.78));
}

.elder-card__avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a90c2;
  background: linear-gradient(135deg, #d7ebf8, #e8f4fc);
  margin-bottom: 12px;
}

.elder-card--green .elder-card__avatar {
  color: #4f9a72;
  background: linear-gradient(135deg, #d8efe3, #eaf7f0);
}

.elder-card--orange .elder-card__avatar {
  color: #c8782a;
  background: linear-gradient(135deg, #ffe8d0, #fff3e6);
}

.elder-card__name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #2c4a5e;
}

.elder-card__meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.elder-card__phone {
  margin: 10px 0 0;
  font-size: 13px;
  color: #4a6072;
}

.elder-card__status {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.elder-card__status.is-ok {
  background: rgba(105, 185, 140, 0.16);
  color: #4f9a72;
}

.elder-card__status.is-off {
  background: rgba(138, 160, 181, 0.18);
  color: #718096;
}

.elder-card__link {
  margin-top: 16px;
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.elder-card__link:hover {
  color: #3a78a8;
}
</style>

<script setup>
import { Female, Male, User } from '@element-plus/icons-vue'
import { calcAge, genderText } from '@/utils/familyHome'

defineProps({
  elders: { type: Array, default: () => [] },
  modelValue: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:modelValue'])

function avatarIcon(gender) {
  if (gender === 1) return Male
  if (gender === 2) return Female
  return User
}

function meta(elder) {
  const age = calcAge(elder.birthDate)
  const gender = genderText(elder.gender)
  return age != null ? `${gender} · ${age}岁` : gender
}
</script>

<template>
  <div class="elder-select">
    <el-empty
      v-if="!elders.length"
      description="暂无可预约服务的老人"
      :image-size="72"
    >
      <template #description>
        <p class="empty-title">暂无可预约服务的老人</p>
        <p class="empty-desc">您当前没有可预约服务的老人，请先完成老人档案绑定。</p>
      </template>
    </el-empty>
    <div v-else class="elder-select__grid">
      <button
        v-for="elder in elders"
        :key="elder.id"
        type="button"
        class="elder-option"
        :class="{ 'is-active': modelValue === elder.id }"
        @click="emit('update:modelValue', elder.id)"
      >
        <span class="elder-option__avatar">
          <el-icon :size="22"><component :is="avatarIcon(elder.gender)" /></el-icon>
        </span>
        <span class="elder-option__text">
          <strong>{{ elder.name }}</strong>
          <small>{{ meta(elder) }}</small>
        </span>
        <span v-if="modelValue === elder.id" class="elder-option__check">已选择</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.elder-select__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.elder-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(74, 144, 194, 0.16);
  background: rgba(234, 244, 251, 0.55);
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.elder-option:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(74, 144, 194, 0.12);
}

.elder-option.is-active {
  border-color: rgba(74, 144, 194, 0.4);
  background: linear-gradient(135deg, rgba(74, 144, 194, 0.16), rgba(105, 185, 140, 0.14));
}

.elder-option__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4a90c2;
  background: linear-gradient(135deg, #d7ebf8, #e8f4fc);
  flex-shrink: 0;
}

.elder-option__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.elder-option__text strong {
  font-size: 14px;
  color: #2c4a5e;
}

.elder-option__text small {
  font-size: 12px;
  color: #718096;
}

.elder-option__check {
  font-size: 12px;
  font-weight: 600;
  color: #4f9a72;
  background: rgba(105, 185, 140, 0.16);
  padding: 2px 8px;
  border-radius: 999px;
}

.empty-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a6072;
}

.empty-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .elder-select__grid {
    grid-template-columns: 1fr;
  }
}
</style>

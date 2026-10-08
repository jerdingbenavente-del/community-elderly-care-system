<script setup>
import { computed } from 'vue'
import { Female, Male, User } from '@element-plus/icons-vue'
import { calcAge, genderText } from '@/utils/familyHome'

const props = defineProps({
  elders: { type: Array, default: () => [] },
  modelValue: { type: [Number, String], default: null },
  loading: Boolean,
})

const emit = defineEmits(['update:modelValue'])

const selectedId = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

function avatarIcon(gender) {
  if (gender === 1) return Male
  if (gender === 2) return Female
  return User
}

function label(elder) {
  const age = calcAge(elder.birthDate)
  const gender = genderText(elder.gender)
  const agePart = age != null ? `${age}岁` : ''
  return [gender, agePart].filter(Boolean).join(' · ')
}
</script>

<template>
  <section class="elder-picker">
    <div class="elder-picker__head">
      <h3>选择老人</h3>
      <span>切换后同步刷新健康数据</span>
    </div>
    <el-skeleton v-if="loading" :rows="2" animated />
    <el-empty v-else-if="!elders.length" description="暂未绑定老人" :image-size="56" />
    <div v-else class="elder-picker__list">
      <button
        v-for="elder in elders"
        :key="elder.id"
        type="button"
        class="elder-chip"
        :class="{ 'is-active': selectedId === elder.id }"
        @click="selectedId = elder.id"
      >
        <span class="elder-chip__avatar">
          <el-icon :size="20"><component :is="avatarIcon(elder.gender)" /></el-icon>
        </span>
        <span class="elder-chip__text">
          <strong>{{ elder.name }}</strong>
          <small>{{ label(elder) }}</small>
        </span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.elder-picker {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 16px 18px;
}

.elder-picker__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.elder-picker__head h3 {
  margin: 0;
  font-size: 15px;
  color: #2c4a5e;
}

.elder-picker__head span {
  font-size: 12px;
  color: #8aa0b5;
}

.elder-picker__list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.elder-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 160px;
  padding: 10px 14px;
  border-radius: 14px;
  border: 1px solid rgba(74, 144, 194, 0.14);
  background: rgba(234, 244, 251, 0.55);
  cursor: pointer;
  text-align: left;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.elder-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(74, 144, 194, 0.12);
}

.elder-chip.is-active {
  background: linear-gradient(135deg, rgba(74, 144, 194, 0.18), rgba(105, 185, 140, 0.16));
  border-color: rgba(74, 144, 194, 0.35);
  box-shadow: 0 8px 18px rgba(74, 144, 194, 0.14);
}

.elder-chip__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4a90c2;
  background: linear-gradient(135deg, #d7ebf8, #e8f4fc);
  flex-shrink: 0;
}

.elder-chip__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.elder-chip__text strong {
  font-size: 14px;
  color: #2c4a5e;
}

.elder-chip__text small {
  font-size: 12px;
  color: #718096;
}
</style>

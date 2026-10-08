<script setup>
import { computed } from 'vue'
import { calcAge, genderText, formatDateTime } from '@/utils/familyHome'
import { maskIdCard, maskPhone } from '@/utils/sensitive'

const props = defineProps({
  elder: { type: Object, default: null },
})

const rows = computed(() => {
  const e = props.elder
  if (!e) return []
  return [
    { label: '姓名', value: e.name || '-' },
    { label: '性别', value: genderText(e.gender) },
    { label: '出生日期', value: e.birthDate || '-' },
    { label: '年龄', value: calcAge(e.birthDate) != null ? `${calcAge(e.birthDate)} 岁` : '-' },
    { label: '身份证号', value: maskIdCard(e.idCard) },
    { label: '联系电话', value: maskPhone(e.phone) },
    { label: '地址', value: e.address || '-' },
    { label: '入住日期', value: e.registeredAt || '-' },
    { label: '护理等级', value: e.careLevel || '-' },
    { label: '病史', value: e.medicalHistory || '-' },
    { label: '过敏史', value: e.allergyHistory || '-' },
    { label: '特殊照护', value: e.specialCareRequirement || '-' },
    { label: '备注', value: e.remark || '-' },
    { label: '登记时间', value: formatDateTime(e.createdAt) },
  ]
})
</script>

<template>
  <section class="profile-card">
    <h3 class="profile-card__title">基本信息</h3>
    <el-empty v-if="!elder" description="暂无档案信息" :image-size="64" />
    <dl v-else class="profile-grid">
      <div v-for="row in rows" :key="row.label" class="profile-item">
        <dt>{{ row.label }}</dt>
        <dd>{{ row.value }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.profile-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.profile-card__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.profile-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.profile-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.45);
}

.profile-item dt {
  font-size: 12px;
  color: #8aa0b5;
  margin-bottom: 4px;
}

.profile-item dd {
  margin: 0;
  font-size: 14px;
  color: #2c4a5e;
  word-break: break-all;
}

@media (max-width: 720px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup>
import { maskPhone } from '@/utils/sensitive'

defineProps({
  contacts: { type: Array, default: () => [] },
  loading: Boolean,
})
</script>

<template>
  <section class="contact-card">
    <h3 class="contact-card__title">紧急联系人</h3>
    <el-skeleton v-if="loading" :rows="3" animated />
    <el-empty
      v-else-if="!contacts.length"
      description="暂无紧急联系人"
      :image-size="64"
    >
      <template #description>
        <p class="empty-title">暂无紧急联系人</p>
        <p class="empty-desc">如需补充，请联系服务中心管理员。</p>
      </template>
    </el-empty>
    <ul v-else class="contact-list">
      <li v-for="item in contacts" :key="item.id" class="contact-item">
        <div class="contact-item__main">
          <strong>{{ item.name || '-' }}</strong>
          <span class="contact-item__rel">{{ item.relationship || '亲属' }}</span>
        </div>
        <div class="contact-item__phone">📞 {{ maskPhone(item.phone) }}</div>
        <div v-if="item.priority != null" class="contact-item__priority">
          优先级 {{ item.priority }}
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.contact-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.contact-card__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.contact-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contact-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(238, 247, 243, 0.7), rgba(234, 244, 251, 0.65));
}

.contact-item__main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-item__main strong {
  font-size: 14px;
  color: #2c4a5e;
}

.contact-item__rel {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(74, 144, 194, 0.12);
  color: #4a90c2;
}

.contact-item__phone {
  margin-top: 8px;
  font-size: 13px;
  color: #4a6072;
}

.contact-item__priority {
  margin-top: 4px;
  font-size: 12px;
  color: #8aa0b5;
}

.empty-title {
  margin: 0;
  color: #4a6072;
  font-size: 14px;
}

.empty-desc {
  margin: 6px 0 0;
  color: #8aa0b5;
  font-size: 12px;
}
</style>

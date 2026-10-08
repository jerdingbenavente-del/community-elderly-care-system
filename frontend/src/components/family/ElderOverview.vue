<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Avatar } from '@element-plus/icons-vue'
import { calcAge, genderText } from '@/utils/familyHome'

const props = defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  elders: { type: Array, default: () => [] },
  /** 是否有待处理预警（用于健康状态标签，非医疗诊断） */
  hasUnhandledWarning: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()

const primary = computed(() => {
  const e = (props.elders || [])[0]
  if (!e) return null
  return {
    ...e,
    age: calcAge(e.birthDate),
    genderLabel: genderText(e.gender),
  }
})

const statusText = computed(() =>
  props.hasUnhandledWarning ? '健康状态：关注' : '健康状态：正常',
)

function goDetail() {
  router.push('/family/elders')
}
</script>

<template>
  <section class="info-card">
    <div class="info-card__head">
      <h3>我的老人</h3>
      <button type="button" class="info-card__link" @click="goDetail">查看全部 →</button>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <el-alert v-else-if="error" type="error" :title="error" :closable="false" show-icon />
    <el-empty v-else-if="!primary" description="暂未绑定老人" :image-size="64" />
    <div v-else class="elder" @click="goDetail">
      <div class="elder__avatar">
        <el-icon :size="30"><Avatar /></el-icon>
      </div>
      <div class="elder__meta">
        <div class="elder__name">{{ primary.name }}</div>
        <div class="elder__sub">
          {{ primary.genderLabel }}
          <template v-if="primary.age != null"> · {{ primary.age }} 岁</template>
        </div>
        <span
          class="elder__status"
          :class="hasUnhandledWarning ? 'is-warn' : 'is-ok'"
        >
          {{ statusText }}
        </span>
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

.elder {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(234, 244, 251, 0.55);
  cursor: pointer;
}

.elder__avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(135deg, #d7ebf8, #dcefe4);
  color: #4a90c2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.elder__name {
  font-size: 16px;
  font-weight: 700;
  color: #2c4a5e;
}

.elder__sub {
  margin-top: 4px;
  font-size: 12px;
  color: #718096;
}

.elder__status {
  display: inline-flex;
  margin-top: 8px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.elder__status.is-ok {
  background: rgba(105, 185, 140, 0.16);
  color: #4f9a72;
}

.elder__status.is-warn {
  background: rgba(242, 166, 90, 0.18);
  color: #c8782a;
}
</style>

<script setup>
import { useRouter } from 'vue-router'
import { User, Monitor, Bell, FirstAidKit, Document, Star } from '@element-plus/icons-vue'

const router = useRouter()

const actions = [
  {
    title: '我的老人',
    desc: '查看老人档案与基本信息',
    path: '/family/elders',
    icon: User,
    tone: 'blue',
  },
  {
    title: '健康管理',
    desc: '查看健康记录与健康趋势',
    path: '/family/health',
    icon: Monitor,
    tone: 'green',
  },
  {
    title: '健康预警',
    desc: '关注异常指标及时处理',
    path: '/family/health',
    icon: Bell,
    tone: 'orange',
  },
  {
    title: '照护服务',
    desc: '预约服务查看服务项目',
    path: '/family/care-services',
    icon: FirstAidKit,
    tone: 'purple',
  },
  {
    title: '我的订单',
    desc: '查看订单状态与服务进度',
    path: '/family/orders',
    icon: Document,
    tone: 'teal',
  },
  {
    title: '服务评价',
    desc: '评价服务质量分享体验',
    path: '/family/evaluations',
    icon: Star,
    tone: 'yellow',
  },
]

function go(path) {
  router.push(path)
}
</script>

<template>
  <section class="quick-actions">
    <h3 class="section-title">快捷服务</h3>
    <div class="quick-actions__grid">
      <button
        v-for="item in actions"
        :key="item.title"
        type="button"
        class="action-card"
        @click="go(item.path)"
      >
        <span class="action-card__icon" :class="`is-${item.tone}`">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
        </span>
        <span class="action-card__text">
          <span class="action-card__title">{{ item.title }}</span>
          <span class="action-card__desc">{{ item.desc }}</span>
        </span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
  color: #2c4a5e;
}

.quick-actions__grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.action-card {
  min-height: 124px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 14px;
  padding: 16px 14px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  box-shadow: var(--ec-shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--ec-shadow-hover);
}

.action-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.action-card__icon.is-blue {
  background: linear-gradient(135deg, #4a90c2, #7eb6de);
}
.action-card__icon.is-green {
  background: linear-gradient(135deg, #69b98c, #8fcea8);
}
.action-card__icon.is-orange {
  background: linear-gradient(135deg, #f2a65a, #f6c28a);
}
.action-card__icon.is-purple {
  background: linear-gradient(135deg, #9b7ed9, #b9a0e8);
}
.action-card__icon.is-teal {
  background: linear-gradient(135deg, #5bb8b0, #86d0ca);
}
.action-card__icon.is-yellow {
  background: linear-gradient(135deg, #e6c35c, #f0d687);
}

.action-card__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-card__title {
  font-size: 14px;
  font-weight: 700;
  color: #2c4a5e;
}

.action-card__desc {
  font-size: 12px;
  line-height: 1.45;
  color: #718096;
}

@media (max-width: 1280px) {
  .quick-actions__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .quick-actions__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

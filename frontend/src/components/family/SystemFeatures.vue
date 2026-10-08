<script setup>
import { Monitor, Service, ChatDotRound, Lock } from '@element-plus/icons-vue'

const features = [
  {
    title: '健康监测',
    desc: '血压、体温、心率等指标集中查看，异常及时提醒。',
    icon: Monitor,
    tone: 'blue',
  },
  {
    title: '专业照护',
    desc: '在线预约照护项目，跟踪服务进度与完成情况。',
    icon: Service,
    tone: 'green',
  },
  {
    title: '家属协同',
    desc: '家人随时了解老人近况，减少信息沟通成本。',
    icon: ChatDotRound,
    tone: 'orange',
  },
  {
    title: '安全可靠',
    desc: '身份认证与权限隔离，保护老人与家庭隐私数据。',
    icon: Lock,
    tone: 'teal',
  },
]
</script>

<template>
  <section class="features">
    <div class="section-head">
      <h3 class="section-title">系统特色</h3>
      <p class="section-sub">让照护更专业，让关爱更近距离</p>
    </div>
    <div class="features__grid">
      <article
        v-for="item in features"
        :key="item.title"
        class="feature-card"
        :class="`feature-card--${item.tone}`"
      >
        <span class="feature-card__icon">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </span>
        <h4 class="feature-card__title">{{ item.title }}</h4>
        <p class="feature-card__desc">{{ item.desc }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.section-head {
  margin-bottom: 14px;
}

.section-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: var(--ec-text);
}

.section-sub {
  margin: 0;
  font-size: 13px;
  color: var(--ec-text-muted);
}

.features__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  border: 1px solid var(--ec-border);
  box-shadow: var(--ec-shadow);
  padding: 20px 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--ec-shadow-hover);
}

.feature-card__icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 12px;
}

.feature-card--blue .feature-card__icon {
  background: linear-gradient(135deg, #409eff, #79bbff);
}
.feature-card--green .feature-card__icon {
  background: linear-gradient(135deg, #67c23a, #95d475);
}
.feature-card--orange .feature-card__icon {
  background: linear-gradient(135deg, #ff9f43, #ffb976);
}
.feature-card--teal .feature-card__icon {
  background: linear-gradient(135deg, #36cfc9, #5cdbd3);
}

.feature-card__title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--ec-text);
}

.feature-card__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ec-text-secondary);
}

@media (max-width: 960px) {
  .features__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .features__grid {
    grid-template-columns: 1fr;
  }
}
</style>

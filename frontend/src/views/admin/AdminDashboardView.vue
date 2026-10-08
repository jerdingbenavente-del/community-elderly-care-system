<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Document,
  FirstAidKit,
  Refresh,
  Setting,
  Star,
  User,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStatistics } from '@/api/dashboard'
import bannerImg from '@/assets/images/banner1-family.jpg'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')

const stats = ref({
  elderCount: null,
  careStaffCount: null,
  serviceItemCount: null,
  enabledServiceItemCount: null,
  serviceOrderCount: null,
  warningCount: null,
  unhandledWarningCount: null,
  handledWarningCount: null,
  evaluationCount: null,
  averageScore: null,
})

const orderStatus = ref({
  PENDING: null,
  CONFIRMED: null,
  IN_SERVICE: null,
  COMPLETED: null,
  CANCELLED: null,
})

const recentOrders = ref([])
const recentWarnings = ref([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayText = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

const displayName = computed(() => userStore.displayName)

const averageScoreText = computed(() => {
  const n = stats.value.evaluationCount
  const avg = stats.value.averageScore
  if (n === null || n === undefined) return '--'
  if (Number(n) === 0 || avg === null || avg === undefined) return '暂无'
  const num = Number(avg)
  if (!Number.isFinite(num)) return '暂无'
  return num.toFixed(1)
})

const statusLabels = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusColors = {
  PENDING: '#f0a35a',
  CONFIRMED: '#3f8fc4',
  IN_SERVICE: '#4eb3b0',
  COMPLETED: '#56b49a',
  CANCELLED: '#a0aec0',
}

const shortcuts = [
  { path: '/admin/elders', title: '老人管理', desc: '档案与在册管理', icon: User, color: '#3f8fc4' },
  { path: '/admin/staff', title: '护理员管理', desc: '人员与工号档案', icon: UserFilled, color: '#56b49a' },
  { path: '/admin/users', title: '账号管理', desc: '系统用户与角色', icon: Setting, color: '#8f7bc8' },
  { path: '/admin/services', title: '照护服务', desc: '服务项目维护', icon: FirstAidKit, color: '#4eb3b0' },
  { path: '/admin/orders', title: '服务订单', desc: '预约与执行闭环', icon: Document, color: '#4eb3b0' },
  { path: '/admin/schedules', title: '排班管理', desc: '护理员可服务时段', icon: Calendar, color: '#3f8fc4' },
  { path: '/admin/warnings', title: '健康预警', desc: '异常指标待处理', icon: Warning, color: '#f0a35a' },
  { path: '/admin/evaluations', title: '服务评价', desc: '家属评价查看', icon: Star, color: '#e2c05c' },
]

function displayNum(v) {
  return v === null || v === undefined ? '--' : v
}

function statusLabel(code) {
  return statusLabels[code] || code || '-'
}

function indicatorLabel(code) {
  const map = {
    BLOOD_PRESSURE: '血压',
    BLOOD_GLUCOSE: '血糖',
    BODY_TEMPERATURE: '体温',
    HEART_RATE: '心率',
  }
  return map[code] || code || '-'
}

function resetState() {
  stats.value = {
    elderCount: null,
    careStaffCount: null,
    serviceItemCount: null,
    enabledServiceItemCount: null,
    serviceOrderCount: null,
    warningCount: null,
    unhandledWarningCount: null,
    handledWarningCount: null,
    evaluationCount: null,
    averageScore: null,
  }
  Object.keys(orderStatus.value).forEach((k) => {
    orderStatus.value[k] = null
  })
  recentOrders.value = []
  recentWarnings.value = []
}

async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getDashboardStatistics()
    if (res?.code !== 200) throw new Error(res?.message || '统计数据加载失败')
    const d = res.data || {}
    stats.value = {
      elderCount: d.elderCount ?? 0,
      careStaffCount: d.careStaffCount ?? 0,
      serviceItemCount: d.serviceItemCount ?? 0,
      enabledServiceItemCount: d.enabledServiceItemCount ?? 0,
      serviceOrderCount: d.serviceOrderCount ?? 0,
      warningCount: d.warningCount ?? 0,
      unhandledWarningCount: d.unhandledWarningCount ?? 0,
      handledWarningCount: d.handledWarningCount ?? 0,
      evaluationCount: d.evaluationCount ?? 0,
      averageScore: d.averageScore ?? null,
    }
    orderStatus.value = {
      PENDING: d.pendingOrderCount ?? 0,
      CONFIRMED: d.confirmedOrderCount ?? 0,
      IN_SERVICE: d.inServiceOrderCount ?? 0,
      COMPLETED: d.completedOrderCount ?? 0,
      CANCELLED: d.cancelledOrderCount ?? 0,
    }
    recentOrders.value = d.recentOrders || []
    recentWarnings.value = d.recentWarnings || []
  } catch (e) {
    loadError.value = e.message || '统计数据加载失败'
    resetState()
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <section class="hero-banner">
      <div class="hero-banner__copy">
        <p class="hero-banner__hi">{{ greeting }}，{{ displayName }}</p>
        <h1>用心守护每一位老人</h1>
        <p class="hero-banner__sub">
          让每一位老人都能享受安全 · 健康 · 有尊严的晚年生活
        </p>
        <span class="hero-banner__date">{{ todayText }}</span>
      </div>
      <div
        class="hero-banner__media"
        :style="{ backgroundImage: `url(${bannerImg})` }"
        aria-hidden="true"
      />
      <span class="hero-banner__leaf hero-banner__leaf--l" aria-hidden="true" />
      <span class="hero-banner__leaf hero-banner__leaf--r" aria-hidden="true" />
    </section>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="dash-alert"
      :title="loadError"
    >
      <template #default>
        <el-button type="primary" link :icon="Refresh" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <section class="stat-grid">
      <article class="stat-card tone-blue">
        <div class="stat-card__icon"><el-icon :size="22"><User /></el-icon></div>
        <div>
          <p class="stat-card__label">老人总数</p>
          <strong>{{ displayNum(stats.elderCount) }}</strong>
          <span>当前系统登记老人数量</span>
        </div>
      </article>
      <article class="stat-card tone-green">
        <div class="stat-card__icon"><el-icon :size="22"><UserFilled /></el-icon></div>
        <div>
          <p class="stat-card__label">护理员数量</p>
          <strong>{{ displayNum(stats.careStaffCount) }}</strong>
          <span>在册照护人员档案</span>
        </div>
      </article>
      <article class="stat-card tone-purple">
        <div class="stat-card__icon"><el-icon :size="22"><Document /></el-icon></div>
        <div>
          <p class="stat-card__label">服务订单</p>
          <strong>{{ displayNum(stats.serviceOrderCount) }}</strong>
          <span>全部服务预约订单</span>
        </div>
      </article>
      <article class="stat-card tone-orange">
        <div class="stat-card__icon"><el-icon :size="22"><Warning /></el-icon></div>
        <div>
          <p class="stat-card__label">待处理预警</p>
          <strong>{{ displayNum(stats.unhandledWarningCount) }}</strong>
          <span>未处理健康预警</span>
        </div>
      </article>
    </section>

    <section class="stat-grid secondary">
      <article class="stat-card compact tone-teal">
        <div class="stat-card__icon"><el-icon :size="18"><FirstAidKit /></el-icon></div>
        <div>
          <p class="stat-card__label">可用服务项目</p>
          <strong>{{ displayNum(stats.enabledServiceItemCount) }}</strong>
          <span>ENABLED / 全部 {{ displayNum(stats.serviceItemCount) }}</span>
        </div>
      </article>
      <article class="stat-card compact tone-pink">
        <div class="stat-card__icon"><el-icon :size="18"><Warning /></el-icon></div>
        <div>
          <p class="stat-card__label">预警总数</p>
          <strong>{{ displayNum(stats.warningCount) }}</strong>
          <span>已处理 {{ displayNum(stats.handledWarningCount) }}</span>
        </div>
      </article>
      <article class="stat-card compact tone-blue">
        <div class="stat-card__icon"><el-icon :size="18"><Star /></el-icon></div>
        <div>
          <p class="stat-card__label">服务评价</p>
          <strong>{{ displayNum(stats.evaluationCount) }}</strong>
          <span>累计评价条数</span>
        </div>
      </article>
      <article class="stat-card compact tone-yellow">
        <div class="stat-card__icon"><el-icon :size="18"><Star /></el-icon></div>
        <div>
          <p class="stat-card__label">平均评分</p>
          <strong>{{ averageScoreText }}</strong>
          <span>数据库 AVG(score)，满分 5</span>
        </div>
      </article>
    </section>

    <section class="panel-grid">
      <div class="panel">
        <div class="panel__head">
          <h3>订单状态概览</h3>
          <span>后端 COUNT 聚合</span>
        </div>
        <div class="status-list">
          <div v-for="(label, key) in statusLabels" :key="key" class="status-row">
            <div class="status-row__meta">
              <i :style="{ background: statusColors[key] }" />
              <span>{{ label }}</span>
            </div>
            <strong>{{ displayNum(orderStatus[key]) }}</strong>
            <el-progress
              :percentage="
                stats.serviceOrderCount > 0 && orderStatus[key] != null
                  ? Math.min(100, Math.round((orderStatus[key] / stats.serviceOrderCount) * 100))
                  : 0
              "
              :stroke-width="8"
              :show-text="false"
              :color="statusColors[key]"
            />
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel__head">
          <h3>最近服务订单</h3>
          <span>最新 5 条</span>
        </div>
        <div v-if="!recentOrders.length" class="empty">
          <el-icon :size="36" class="empty__icon"><Document /></el-icon>
          <strong>暂无服务订单</strong>
          <p>新的服务订单将在这里显示</p>
        </div>
        <ul v-else class="list">
          <li v-for="item in recentOrders" :key="item.id">
            <div>
              <strong>{{ item.elderName || '-' }}</strong>
              <p>{{ item.serviceName || '-' }}</p>
            </div>
            <div class="list__right">
              <span>{{ item.scheduledStartTime || '-' }}</span>
              <em>{{ statusLabel(item.status) }}</em>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <h3>最近异常预警</h3>
        <span>最新 5 条</span>
      </div>
      <div v-if="!recentWarnings.length" class="empty">
        <el-icon :size="36" class="empty__icon empty__icon--warn"><Warning /></el-icon>
        <strong>暂无异常预警</strong>
        <p>健康异常预警出现后将在这里展示</p>
      </div>
      <ul v-else class="list">
        <li v-for="item in recentWarnings" :key="item.id">
          <div>
            <strong>{{ item.elderName || '-' }}</strong>
            <p>{{ indicatorLabel(item.indicator) }} · {{ item.warningLevel || '-' }}</p>
          </div>
          <div class="list__right">
            <span>{{ item.generatedAt || '-' }}</span>
            <em>{{ item.status === 'UNHANDLED' ? '未处理' : item.status === 'HANDLED' ? '已处理' : item.status }}</em>
          </div>
        </li>
      </ul>
    </section>

    <section class="panel">
      <div class="panel__head">
        <h3>快捷入口</h3>
        <span>进入对应业务管理页</span>
      </div>
      <div class="shortcut-grid">
        <button
          v-for="item in shortcuts"
          :key="item.path"
          type="button"
          class="shortcut"
          @click="go(item.path)"
        >
          <span class="shortcut__icon" :style="{ background: item.color }">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </span>
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: dashIn 0.35s ease both;
}

.hero-banner {
  position: relative;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  min-height: 168px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(115deg, #e8f4fb 0%, #f0faf6 55%, #dff0f8 100%);
  box-shadow: var(--ec-shadow);
  border: 1px solid rgba(63, 143, 196, 0.08);
}

.hero-banner__copy {
  position: relative;
  z-index: 2;
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-banner__hi {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--ec-color-primary-dark);
  font-weight: 500;
}

.hero-banner__copy h1 {
  margin: 0 0 10px;
  font-size: 26px;
  color: #2a5f86;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.hero-banner__sub {
  margin: 0 0 14px;
  font-size: 14px;
  color: #5a7a90;
  line-height: 1.55;
  max-width: 420px;
}

.hero-banner__date {
  display: inline-flex;
  align-self: flex-start;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(63, 143, 196, 0.12);
  color: var(--ec-color-primary-dark);
  font-size: 12px;
}

.hero-banner__media {
  position: relative;
  z-index: 1;
  background-size: cover;
  background-position: center 35%;
  background-repeat: no-repeat;
  mask-image: linear-gradient(90deg, transparent 0%, #000 22%);
}

.hero-banner__leaf {
  position: absolute;
  z-index: 2;
  width: 48px;
  height: 72px;
  border-radius: 60% 40% 55% 45%;
  background: rgba(86, 180, 154, 0.22);
  pointer-events: none;
}

.hero-banner__leaf--l {
  left: -8px;
  bottom: -10px;
  transform: rotate(-28deg);
}

.hero-banner__leaf--r {
  right: 38%;
  top: -16px;
  width: 36px;
  height: 52px;
  background: rgba(63, 143, 196, 0.14);
  transform: rotate(18deg);
}

.dash-alert {
  border-radius: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  position: relative;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--ec-shadow);
  border: 1px solid rgba(63, 143, 196, 0.06);
  overflow: hidden;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ec-shadow-hover);
}

.stat-card.compact {
  min-height: auto;
}

.stat-card::after {
  content: '';
  position: absolute;
  right: -12px;
  bottom: -18px;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--tone) 10%, transparent);
  pointer-events: none;
}

.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 14%, transparent);
  flex-shrink: 0;
}

.tone-blue { --tone: #3f8fc4; }
.tone-green { --tone: #56b49a; }
.tone-purple { --tone: #8f7bc8; }
.tone-orange { --tone: #f0a35a; }
.tone-teal { --tone: #4eb3b0; }
.tone-pink { --tone: #e88a8a; }
.tone-yellow { --tone: #d4a94a; }

.stat-card__label {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--ec-text-secondary);
}

.stat-card strong {
  display: block;
  font-size: 28px;
  line-height: 1.2;
  color: var(--ec-text);
}

.stat-card span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.panel-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 14px;
}

.panel {
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--ec-shadow);
  border: 1px solid rgba(63, 143, 196, 0.06);
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.panel__head h3 {
  margin: 0;
  font-size: 15px;
  color: var(--ec-text);
}

.panel__head span {
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-row {
  display: grid;
  grid-template-columns: 110px 48px 1fr;
  gap: 10px;
  align-items: center;
}

.status-row__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ec-text);
}

.status-row__meta i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-row strong {
  text-align: right;
  font-size: 15px;
  color: var(--ec-text);
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(63, 143, 196, 0.08);
}

.list li:last-child {
  border-bottom: none;
}

.list strong {
  display: block;
  font-size: 14px;
  color: var(--ec-text);
}

.list p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.list__right {
  text-align: right;
  flex-shrink: 0;
}

.list__right span {
  display: block;
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.list__right em {
  display: inline-block;
  margin-top: 4px;
  font-style: normal;
  font-size: 12px;
  color: var(--ec-color-primary-dark);
}

.empty {
  padding: 28px 12px;
  text-align: center;
  color: var(--ec-text-secondary);
}

.empty__icon {
  color: var(--ec-color-primary);
  opacity: 0.45;
  margin-bottom: 8px;
}

.empty__icon--warn {
  color: var(--ec-color-warn);
}

.empty strong {
  display: block;
  font-size: 14px;
  color: var(--ec-text);
  margin-bottom: 4px;
}

.empty p {
  margin: 0;
  font-size: 12px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(63, 143, 196, 0.06);
  border-radius: 14px;
  background: #f7fbfd;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease, transform 0.15s ease;
}

.shortcut:hover {
  background: #eef6fb;
  transform: translateY(-1px);
}

.shortcut__icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
}

.shortcut strong {
  font-size: 14px;
  color: var(--ec-text);
}

.shortcut p {
  margin: 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
  line-height: 1.4;
}

@keyframes dashIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1100px) {
  .stat-grid,
  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }

  .hero-banner {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .hero-banner__media {
    min-height: 120px;
    mask-image: linear-gradient(180deg, transparent 0%, #000 30%);
  }
}

@media (max-width: 640px) {
  .stat-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .hero-banner__copy {
    padding: 20px;
  }

  .hero-banner__copy h1 {
    font-size: 22px;
  }
}
</style>

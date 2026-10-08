<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  Calendar,
  Clock,
  Dish,
  Document,
  FirstAidKit,
  Refresh,
  Search,
  User,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'
import { getOperationsOverview } from '@/api/statistics'
import { toastIfNeeded } from '@/api/request'
import { formatDate, mondayOf } from '@/utils/week'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const loadError = ref('')
const chartRef = ref(null)
let chart = null

const rangeMode = ref('THIS_MONTH')
const custom = reactive({ dateFrom: '', dateTo: '' })

const data = ref(null)

const today = () => formatDate(new Date())

function rangePreset(mode) {
  const t = new Date()
  const tStr = formatDate(t)
  if (mode === 'TODAY') {
    return { dateFrom: tStr, dateTo: tStr }
  }
  if (mode === 'THIS_WEEK') {
    return { dateFrom: formatDate(mondayOf(t)), dateTo: tStr }
  }
  // THIS_MONTH
  const from = new Date(t.getFullYear(), t.getMonth(), 1)
  return { dateFrom: formatDate(from), dateTo: tStr }
}

function displayNum(v) {
  if (v === null || v === undefined) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function rateText(rate) {
  if (rate === null || rate === undefined) return '0%'
  const n = Number(rate)
  if (!Number.isFinite(n)) return '0%'
  return `${(n * 100).toFixed(1)}%`
}

function avgText(avg, total) {
  if (!total) return '暂无'
  if (avg === null || avg === undefined) return '暂无'
  const n = Number(avg)
  return Number.isFinite(n) ? n.toFixed(1) : '暂无'
}

const elder = computed(() => data.value?.elder || {})
const orders = computed(() => data.value?.orders || {})
const evaluation = computed(() => data.value?.evaluation || {})
const careStaff = computed(() => data.value?.careStaff || {})
const attendance = computed(() => data.value?.attendance || {})
const leave = computed(() => data.value?.leave || {})
const medication = computed(() => data.value?.medication || {})
const menu = computed(() => data.value?.menu || {})
const activity = computed(() => data.value?.activity || {})
const trend = computed(() => data.value?.trend || [])

async function load() {
  if (rangeMode.value === 'CUSTOM') {
    if (!custom.dateFrom || !custom.dateTo) {
      ElMessage.warning('请选择开始与结束日期')
      return
    }
    if (custom.dateFrom > custom.dateTo) {
      ElMessage.warning('开始日期不能晚于结束日期')
      return
    }
  }
  const params =
    rangeMode.value === 'CUSTOM'
      ? { dateFrom: custom.dateFrom, dateTo: custom.dateTo }
      : rangePreset(rangeMode.value)

  loading.value = true
  loadError.value = ''
  try {
    const res = await getOperationsOverview(params)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    data.value = res.data || {}
    await nextTick()
    renderChart()
  } catch (e) {
    data.value = null
    loadError.value = e.message || '统计加载失败'
    toastIfNeeded(e, '统计加载失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const points = trend.value
  chart.setOption({
    color: ['#3f8fc4', '#56b49a'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单总量', '已完成'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.date),
      boundaryGap: false,
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '订单总量',
        type: 'line',
        smooth: true,
        data: points.map((p) => displayNum(p.total)),
      },
      {
        name: '已完成',
        type: 'line',
        smooth: true,
        data: points.map((p) => displayNum(p.completed)),
      },
    ],
  })
}

function onResize() {
  chart?.resize()
}

watch(rangeMode, (mode) => {
  if (mode !== 'CUSTOM') load()
})

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="stats-page" v-loading="loading">
    <header class="page-head">
      <div>
        <h1>运营统计与综合报表</h1>
        <p>基于现有业务表实时聚合。订单时间口径：预约开始时间 scheduled_start_time。</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </header>

    <section class="panel range-bar">
      <el-radio-group v-model="rangeMode" size="default">
        <el-radio-button value="TODAY">今日</el-radio-button>
        <el-radio-button value="THIS_WEEK">本周</el-radio-button>
        <el-radio-button value="THIS_MONTH">本月</el-radio-button>
        <el-radio-button value="CUSTOM">自定义</el-radio-button>
      </el-radio-group>
      <template v-if="rangeMode === 'CUSTOM'">
        <el-date-picker v-model="custom.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker v-model="custom.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </template>
      <span v-if="data" class="range-label">统计区间：{{ data.dateFrom }} ～ {{ data.dateTo }}</span>
    </section>

    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" class="mb" />

    <section class="stat-grid">
      <article class="stat-card tone-blue">
        <div class="stat-card__icon"><el-icon :size="20"><User /></el-icon></div>
        <div>
          <p class="stat-card__label">老人总数</p>
          <strong>{{ displayNum(elder.total) }}</strong>
          <span>有效 {{ displayNum(elder.active) }} / 停用 {{ displayNum(elder.inactive) }}</span>
        </div>
      </article>
      <article class="stat-card tone-green">
        <div class="stat-card__icon"><el-icon :size="20"><UserFilled /></el-icon></div>
        <div>
          <p class="stat-card__label">护理员总数</p>
          <strong>{{ displayNum(careStaff.total) }}</strong>
          <span>启用 {{ displayNum(careStaff.active) }}</span>
        </div>
      </article>
      <article class="stat-card tone-teal">
        <div class="stat-card__icon"><el-icon :size="20"><Document /></el-icon></div>
        <div>
          <p class="stat-card__label">本期服务订单</p>
          <strong>{{ displayNum(orders.total) }}</strong>
          <span>完成率 {{ rateText(orders.completedRate) }}</span>
        </div>
      </article>
      <article class="stat-card tone-purple">
        <div class="stat-card__icon"><el-icon :size="20"><Document /></el-icon></div>
        <div>
          <p class="stat-card__label">本期完成服务</p>
          <strong>{{ displayNum(orders.completed) }}</strong>
          <span>取消 {{ displayNum(orders.cancelled) }}</span>
        </div>
      </article>
    </section>

    <section class="stat-grid secondary">
      <article class="stat-card compact tone-orange">
        <div class="stat-card__icon"><el-icon :size="18"><Warning /></el-icon></div>
        <div>
          <p class="stat-card__label">当前请假</p>
          <strong>{{ displayNum(leave.currentLeave) }}</strong>
          <span>待审批 {{ displayNum(leave.pendingLeave) }}</span>
        </div>
      </article>
      <article class="stat-card compact tone-blue">
        <div class="stat-card__icon"><el-icon :size="18"><Clock /></el-icon></div>
        <div>
          <p class="stat-card__label">今日考勤</p>
          <strong>{{ displayNum(careStaff.todayCheckedIn) }}</strong>
          <span>已签退 {{ displayNum(careStaff.todayCheckedOut) }}</span>
        </div>
      </article>
      <article class="stat-card compact tone-pink">
        <div class="stat-card__icon"><el-icon :size="18"><FirstAidKit /></el-icon></div>
        <div>
          <p class="stat-card__label">今日用药提醒</p>
          <strong>{{ displayNum(medication.todayReminder) }}</strong>
          <span>到点 DUE {{ displayNum(medication.todayDue) }}</span>
        </div>
      </article>
      <article class="stat-card compact tone-yellow">
        <div class="stat-card__icon"><el-icon :size="18"><Calendar /></el-icon></div>
        <div>
          <p class="stat-card__label">本月活动</p>
          <strong>{{ displayNum(activity.thisMonth) }}</strong>
          <span>即将举行 {{ displayNum(activity.upcoming) }}</span>
        </div>
      </article>
    </section>

    <section class="panel-grid">
      <div class="panel">
        <div class="panel__head">
          <h3>订单状态分布（期间内）</h3>
          <span>有效订单完成率 = COMPLETED / (TOTAL − CANCELLED)</span>
        </div>
        <div class="kv-list">
          <div class="kv"><span>待确认</span><strong>{{ displayNum(orders.pending) }}</strong></div>
          <div class="kv"><span>已确认</span><strong>{{ displayNum(orders.confirmed) }}</strong></div>
          <div class="kv"><span>服务中</span><strong>{{ displayNum(orders.inService) }}</strong></div>
          <div class="kv"><span>已完成</span><strong>{{ displayNum(orders.completed) }}</strong></div>
          <div class="kv"><span>已取消</span><strong>{{ displayNum(orders.cancelled) }}</strong></div>
          <div class="kv"><span>完成率</span><strong>{{ rateText(orders.completedRate) }}</strong></div>
        </div>
      </div>

      <div class="panel">
        <div class="panel__head">
          <h3>老人 / 评价 / 家属绑定</h3>
        </div>
        <div class="kv-list">
          <div class="kv"><span>已绑定家属老人</span><strong>{{ displayNum(elder.boundFamily) }}</strong></div>
          <div class="kv"><span>未绑定家属（有效）</span><strong>{{ displayNum(elder.unboundFamily) }}</strong></div>
          <div class="kv"><span>期间评价数</span><strong>{{ displayNum(evaluation.total) }}</strong></div>
          <div class="kv"><span>平均评分</span><strong>{{ avgText(evaluation.averageScore, evaluation.total) }}</strong></div>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <div class="panel">
        <div class="panel__head">
          <h3>考勤快照</h3>
          <span>日期 {{ attendance.snapshotDate || '-' }}（G6 计算态）</span>
        </div>
        <div class="kv-list">
          <div class="kv"><span>已签到</span><strong>{{ displayNum(attendance.checkedIn) }}</strong></div>
          <div class="kv"><span>已完成签退</span><strong>{{ displayNum(attendance.completed) }}</strong></div>
          <div class="kv"><span>未签到</span><strong>{{ displayNum(attendance.notChecked) }}</strong></div>
          <div class="kv"><span>请假</span><strong>{{ displayNum(attendance.leave) }}</strong></div>
        </div>
      </div>

      <div class="panel">
        <div class="panel__head">
          <h3>用药 / 膳食 / 活动</h3>
        </div>
        <div class="kv-list">
          <div class="kv"><span>ACTIVE 用药计划</span><strong>{{ displayNum(medication.active) }}</strong></div>
          <div class="kv"><span>INACTIVE 用药计划</span><strong>{{ displayNum(medication.inactive) }}</strong></div>
          <div class="kv"><span>本周有效菜单</span><strong>{{ displayNum(menu.currentWeek) }}</strong></div>
          <div class="kv"><span>本周餐次数</span><strong>{{ displayNum(menu.mealCount) }}</strong></div>
          <div class="kv"><span>饮食备注老人数</span><strong>{{ displayNum(menu.dietaryNoteElderCount) }}</strong></div>
          <div class="kv"><span>个性化调整老人数</span><strong>{{ displayNum(menu.adjustedElderCount) }}</strong></div>
          <div class="kv"><span>已发布活动</span><strong>{{ displayNum(activity.published) }}</strong></div>
          <div class="kv"><span>已取消 / 已结束</span><strong>{{ displayNum(activity.cancelled) }} / {{ displayNum(activity.completed) }}</strong></div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <h3>近 7 日服务订单趋势</h3>
        <span>按预约开始日期；无单日为 0</span>
      </div>
      <div ref="chartRef" class="chart" />
    </section>
  </div>
</template>

<style scoped>
.stats-page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.page-head h1 { margin: 0 0 6px; font-size: 22px; color: #1f2a37; }
.page-head p { margin: 0; color: #667085; font-size: 13px; }
.panel { background: #fff; border-radius: 14px; padding: 16px 18px; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04); }
.range-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.range-label { color: #667085; font-size: 13px; }
.mb { margin-bottom: 0; }
.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.stat-grid.secondary { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.stat-card {
  display: flex; gap: 12px; align-items: flex-start;
  background: #fff; border-radius: 14px; padding: 16px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.stat-card.compact { padding: 14px; }
.stat-card__icon {
  width: 40px; height: 40px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0;
}
.stat-card__label { margin: 0; color: #667085; font-size: 13px; }
.stat-card strong { display: block; margin: 4px 0; font-size: 26px; color: #1f2a37; line-height: 1.1; }
.stat-card span { color: #98a2b3; font-size: 12px; }
.tone-blue .stat-card__icon { background: #3f8fc4; }
.tone-green .stat-card__icon { background: #56b49a; }
.tone-teal .stat-card__icon { background: #4eb3b0; }
.tone-purple .stat-card__icon { background: #8f7bc8; }
.tone-orange .stat-card__icon { background: #f0a35a; }
.tone-pink .stat-card__icon { background: #e28aa8; }
.tone-yellow .stat-card__icon { background: #e2c05c; }
.panel-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.panel__head { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; margin-bottom: 12px; }
.panel__head h3 { margin: 0; font-size: 16px; }
.panel__head span { color: #98a2b3; font-size: 12px; }
.kv-list { display: grid; gap: 8px; }
.kv { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f2f4f7; color: #475467; }
.kv strong { color: #1f2a37; }
.chart { width: 100%; height: 320px; }
@media (max-width: 1100px) {
  .stat-grid, .stat-grid.secondary, .panel-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 720px) {
  .stat-grid, .stat-grid.secondary, .panel-grid { grid-template-columns: 1fr; }
}
</style>

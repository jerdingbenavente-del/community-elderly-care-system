<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Document,
  Notebook,
  Refresh,
  Timer,
  User,
  VideoPlay,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  completeMyServiceOrder,
  formatLocalDate,
  pageMySchedules,
  pageMyServiceOrders,
  pageMyServiceRecords,
  startMyServiceOrder,
  todayScheduleWindow,
} from '@/api/staff'
import { staffHeroBanner } from '@/config/staffImages'
import { orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'
import { checkInAttendance, checkOutAttendance, getTodayAttendance } from '@/api/careStaffAttendance'
import { todayMedicationReminders } from '@/api/elderMedication'
import { toastIfNeeded } from '@/api/request'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const actionLoadingId = ref(null)
const attendance = ref(null)
const attendanceActing = ref(false)
const medicationReminders = ref([])
const medicationReminderError = ref('')

const overview = ref({
  todayOrderTotal: null,
  todayConfirmed: null,
  inService: null,
  todaySchedule: null,
})

const todayTasks = ref([])
const weekSchedules = ref([])
const todayShiftRows = ref([])
const latestRecords = ref([])

const displayName = computed(() => userStore.displayName)

const greetText = computed(() => {
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

const todayShiftText = computed(() => {
  const rows = todayShiftRows.value.filter((r) => r.status !== 'CANCELLED')
  if (!rows.length) return '今日暂无排班'
  const first = rows[0]
  const start = formatTime(first.startTime)
  const end = formatTime(first.endTime)
  if (rows.length === 1) return `${start} - ${end}`
  return `${start} - ${end} 等 ${rows.length} 个时段`
})

const pendingTip = computed(() => {
  const n = overview.value.todayConfirmed
  if (n === null || n === undefined) return ''
  if (Number(n) === 0) return '今日暂无待开始服务'
  return `今日有 ${n} 项待开始服务`
})

const reminderItems = computed(() => {
  const items = []
  const o = overview.value
  if (o.todayConfirmed > 0) {
    items.push({ tone: 'orange', text: `有 ${o.todayConfirmed} 项待开始服务，请按时执行` })
  }
  if (o.inService > 0) {
    items.push({ tone: 'blue', text: `有 ${o.inService} 项服务进行中，请及时完成` })
  }
  if (o.todaySchedule > 0) {
    items.push({ tone: 'green', text: `今日排班 ${o.todaySchedule} 个时段：${todayShiftText.value}` })
  }
  if (!items.length && !loadError.value) {
    items.push({ tone: 'muted', text: '今日暂无待办提醒，请保持关注服务安排' })
  }
  return items
})

const quickEntries = [
  { title: '我的服务', path: '/staff/services', icon: Document, tone: 'blue' },
  { title: '服务记录', path: '/staff/service-records', icon: Notebook, tone: 'green' },
  { title: '我的排班', path: '/staff/schedules', icon: Calendar, tone: 'teal' },
  { title: '个人中心', path: '/staff/profile', icon: User, tone: 'orange' },
]

function displayNum(v) {
  return v === null || v === undefined ? '--' : v
}

function formatTime(t) {
  if (!t) return '--'
  return String(t).slice(0, 5)
}

function remindStatusText(status) {
  if (status === 'DUE') return '到点提醒'
  if (status === 'PENDING') return '待服药'
  return status || '-'
}

function canStart(row) {
  return row?.status === 'CONFIRMED'
}

function canComplete(row) {
  return row?.status === 'IN_SERVICE'
}

function statusTone(status) {
  if (status === 'CONFIRMED' || status === 'PENDING') return 'pending'
  if (status === 'IN_SERVICE') return 'doing'
  if (status === 'COMPLETED') return 'done'
  return 'muted'
}

async function fetchTotal(fn, params = {}) {
  const res = await fn({ page: 1, size: 1, ...params })
  if (res?.code !== 200) throw new Error(res?.message || '加载失败')
  return Number(res.data?.total ?? 0)
}

function buildWeekDays() {
  const days = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < 7; i += 1) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    days.push({
      date: formatLocalDate(d),
      label: `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
      week: ['日', '一', '二', '三', '四', '五', '六'][d.getDay()],
      isToday: i === 0,
      slots: [],
    })
  }
  return days
}

async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    const win = todayScheduleWindow()
    const weekDays = buildWeekDays()
    const startDate = weekDays[0].date
    const endDate = weekDays[6].date

    const [
      todayOrderTotal,
      todayConfirmed,
      inService,
      todaySchedule,
      tasksRes,
      schedulesRes,
      recordsRes,
    ] = await Promise.all([
      fetchTotal(pageMyServiceOrders, {
        scheduledStartFrom: win.scheduledStartFrom,
        scheduledStartTo: win.scheduledStartTo,
      }),
      fetchTotal(pageMyServiceOrders, {
        status: 'CONFIRMED',
        scheduledStartFrom: win.scheduledStartFrom,
        scheduledStartTo: win.scheduledStartTo,
      }),
      fetchTotal(pageMyServiceOrders, { status: 'IN_SERVICE' }),
      fetchTotal(pageMySchedules, { scheduleDate: win.date }),
      pageMyServiceOrders({
        page: 1,
        size: 8,
        scheduledStartFrom: win.scheduledStartFrom,
        scheduledStartTo: win.scheduledStartTo,
      }),
      pageMySchedules({ page: 1, size: 50, startDate, endDate }),
      pageMyServiceRecords({ page: 1, size: 5 }),
    ])

    if (tasksRes?.code !== 200) throw new Error(tasksRes?.message || '任务加载失败')
    if (schedulesRes?.code !== 200) throw new Error(schedulesRes?.message || '排班加载失败')
    if (recordsRes?.code !== 200) throw new Error(recordsRes?.message || '记录加载失败')

    overview.value = { todayOrderTotal, todayConfirmed, inService, todaySchedule }
    todayTasks.value = tasksRes.data?.records || []
    latestRecords.value = recordsRes.data?.records || []

    const scheduleRows = (schedulesRes.data?.records || []).filter((r) => r.status !== 'CANCELLED')
    todayShiftRows.value = scheduleRows.filter((r) => r.scheduleDate === win.date)

    const map = Object.fromEntries(weekDays.map((d) => [d.date, d]))
    scheduleRows.forEach((row) => {
      const day = map[row.scheduleDate]
      if (day) day.slots.push(row)
    })
    weekSchedules.value = weekDays
  } catch (e) {
    loadError.value = e.message || '今日工作数据加载失败'
    overview.value = {
      todayOrderTotal: null,
      todayConfirmed: null,
      inService: null,
      todaySchedule: null,
    }
    todayTasks.value = []
    weekSchedules.value = buildWeekDays()
    todayShiftRows.value = []
    latestRecords.value = []
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

function goDetail(row) {
  router.push(`/staff/services/${row.id}`)
}

async function onStart(row) {
  try {
    await ElMessageBox.confirm(
      `确认开始服务订单「${row.orderNo || row.id}」？将变为服务中。`,
      '开始服务',
      { type: 'warning', confirmButtonText: '开始', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoadingId.value = row.id
  try {
    const res = await startMyServiceOrder(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '开始失败')
    ElMessage.success('服务已开始')
    await loadDashboard()
  } catch {
    // request.js 已提示
  } finally {
    actionLoadingId.value = null
  }
}

async function onComplete(row) {
  try {
    await ElMessageBox.confirm(
      `确认完成服务订单「${row.orderNo || row.id}」？完成后不可回退。`,
      '完成服务',
      { type: 'warning', confirmButtonText: '完成', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoadingId.value = row.id
  try {
    const res = await completeMyServiceOrder(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '完成失败')
    ElMessage.success('服务已完成')
    await loadDashboard()
  } catch {
    // request.js 已提示
  } finally {
    actionLoadingId.value = null
  }
}

function dayCardClass(day) {
  if (day.isToday) return 'is-today'
  if (!day.slots.length) return 'is-rest'
  return 'is-work'
}

function dayShiftLabel(day) {
  if (!day.slots.length) return '休息'
  const first = day.slots[0]
  return `${formatTime(first.startTime)}-${formatTime(first.endTime)}`
}

const attendanceStatusMap = {
  NOT_CHECKED: '未签到',
  WORKING: '工作中',
  COMPLETED: '已完成',
  LEAVE: '请假',
}

function attendanceLabel(status) {
  return attendanceStatusMap[status] || '未签到'
}

function fmtAttendanceTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadAttendance() {
  try {
    const res = await getTodayAttendance()
    if (res?.code !== 200) throw new Error(res?.message || '加载今日考勤失败')
    attendance.value = res.data
  } catch (e) {
    attendance.value = null
    toastIfNeeded(e, '加载今日考勤失败')
  }
}

async function doCheckIn() {
  attendanceActing.value = true
  try {
    const res = await checkInAttendance()
    if (res?.code !== 200) throw new Error(res?.message || '签到失败')
    ElMessage.success('上班签到成功')
    await loadAttendance()
  } catch (e) {
    toastIfNeeded(e, '签到失败')
    await loadAttendance()
  } finally {
    attendanceActing.value = false
  }
}

async function doCheckOut() {
  attendanceActing.value = true
  try {
    const res = await checkOutAttendance()
    if (res?.code !== 200) throw new Error(res?.message || '签退失败')
    ElMessage.success('下班签退成功')
    await loadAttendance()
  } catch (e) {
    toastIfNeeded(e, '签退失败')
    await loadAttendance()
  } finally {
    attendanceActing.value = false
  }
}

async function loadMedicationReminders() {
  medicationReminderError.value = ''
  try {
    const res = await todayMedicationReminders()
    if (res?.code !== 200) throw new Error(res?.message || '加载今日用药提醒失败')
    medicationReminders.value = res.data || []
  } catch (e) {
    medicationReminders.value = []
    medicationReminderError.value = e.message || '加载今日用药提醒失败'
    toastIfNeeded(e, '加载今日用药提醒失败')
  }
}

onMounted(async () => {
  await loadDashboard()
  await loadAttendance()
  await loadMedicationReminders()
})
</script>

<template>
  <div class="staff-home" v-loading="loading">
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="alert"
      :title="loadError"
    >
      <template #default>
        <el-button type="primary" link :icon="Refresh" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <!-- 家属端同构：中央主内容 + 右侧信息 -->
    <div class="staff-home__top">
      <section class="staff-home__center">
        <!-- 中央顶部 Banner：12.jpg 仅作卡片，不铺满整页 -->
        <article
          class="hero-card"
          :style="{ '--hero-img': `url(${staffHeroBanner.image})` }"
          aria-label="护理主题 Banner"
        >
          <div class="hero-card__media" />
          <div class="hero-card__date">{{ todayText }}</div>
        </article>

        <section class="welcome-card">
          <div>
            <h3>{{ greetText }}，{{ displayName }}</h3>
            <p>今天请用耐心与专业守护每一位老人。</p>
          </div>
          <div class="welcome-card__shift">今日班次 {{ todayShiftText }}</div>
        </section>

        <section class="panel-card attendance-card">
          <div class="panel-card__head">
            <h2>今日考勤</h2>
            <span>{{ attendance?.attendanceDate || '' }}</span>
          </div>
          <div class="attendance-card__body">
            <div>
              <p>状态 {{ attendanceLabel(attendance?.status) }}</p>
              <p>签到 {{ fmtAttendanceTime(attendance?.checkInTime) }}</p>
              <p>签退 {{ fmtAttendanceTime(attendance?.checkOutTime) }}</p>
            </div>
            <div class="attendance-card__actions">
              <el-button
                v-if="!attendance || attendance.status === 'NOT_CHECKED'"
                type="primary"
                :loading="attendanceActing"
                @click="doCheckIn"
              >上班签到</el-button>
              <el-button
                v-else-if="attendance.status === 'WORKING'"
                type="primary"
                :loading="attendanceActing"
                @click="doCheckOut"
              >下班签退</el-button>
              <el-button v-else-if="attendance.status === 'COMPLETED'" disabled>今日考勤已完成</el-button>
              <el-button v-else-if="attendance.status === 'LEAVE'" disabled>今日请假</el-button>
              <el-button link type="primary" @click="router.push('/staff/attendance')">我的考勤</el-button>
            </div>
          </div>
        </section>

        <section class="panel-card">
          <div class="panel-card__head">
            <h2>今日用药提醒</h2>
            <el-button link type="primary" :icon="Refresh" @click="loadMedicationReminders">刷新</el-button>
          </div>
          <el-alert
            v-if="medicationReminderError"
            type="error"
            :closable="false"
            show-icon
            :title="medicationReminderError"
          />
          <el-table v-else :data="medicationReminders" stripe empty-text="今日暂无用药提醒">
            <el-table-column label="时间" width="80">
              <template #default="{ row }">{{ formatTime(row.doseTime) }}</template>
            </el-table-column>
            <el-table-column prop="elderName" label="老人" min-width="80">
              <template #default="{ row }">{{ row.elderName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="medicineName" label="药品" min-width="120" />
            <el-table-column label="剂量" width="100">
              <template #default="{ row }">{{ row.dosage }}{{ row.dosageUnit }}</template>
            </el-table-column>
            <el-table-column prop="usageMethod" label="方式" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <span class="status-tag" :class="row.remindStatus === 'DUE' ? 'status-tag--doing' : 'status-tag--pending'">
                  {{ remindStatusText(row.remindStatus) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="panel-card">
          <div class="panel-card__head">
            <h2>今日工作概览</h2>
            <span v-if="pendingTip">{{ pendingTip }}</span>
          </div>
          <div class="stat-grid">
            <article class="stat stat--blue">
              <div class="stat__icon"><el-icon :size="18"><Document /></el-icon></div>
              <div>
                <p>今日服务</p>
                <strong>{{ displayNum(overview.todayOrderTotal) }}</strong>
              </div>
            </article>
            <article class="stat stat--orange">
              <div class="stat__icon"><el-icon :size="18"><Timer /></el-icon></div>
              <div>
                <p>待开始</p>
                <strong>{{ displayNum(overview.todayConfirmed) }}</strong>
              </div>
            </article>
            <article class="stat stat--teal">
              <div class="stat__icon"><el-icon :size="18"><VideoPlay /></el-icon></div>
              <div>
                <p>服务中</p>
                <strong>{{ displayNum(overview.inService) }}</strong>
              </div>
            </article>
            <article class="stat stat--green">
              <div class="stat__icon"><el-icon :size="18"><Calendar /></el-icon></div>
              <div>
                <p>今日排班</p>
                <strong>{{ displayNum(overview.todaySchedule) }}</strong>
              </div>
            </article>
          </div>
        </section>

        <section class="panel-card">
          <div class="panel-card__head">
            <h2>护理任务</h2>
            <el-button link type="primary" @click="go('/staff/services')">查看全部</el-button>
          </div>
          <el-table :data="todayTasks" stripe empty-text="今日暂无护理任务">
            <el-table-column label="任务类型" min-width="100">
              <template #default="{ row }">
                <span class="type-pill">{{ serviceTypeLabel(row.serviceType) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="elderName" label="老人" min-width="80">
              <template #default="{ row }">{{ row.elderName || '-' }}</template>
            </el-table-column>
            <el-table-column label="任务内容" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.serviceName || '-' }}</template>
            </el-table-column>
            <el-table-column label="计划时间" min-width="140">
              <template #default="{ row }">{{ row.scheduledStartTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="status-tag" :class="`status-tag--${statusTone(row.status)}`">
                  {{ orderStatusMeta(row.status).label }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="168" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="goDetail(row)">查看</el-button>
                <el-button
                  v-if="canStart(row)"
                  link
                  type="success"
                  :loading="actionLoadingId === row.id"
                  @click="onStart(row)"
                >
                  开始
                </el-button>
                <el-button
                  v-if="canComplete(row)"
                  link
                  type="warning"
                  :loading="actionLoadingId === row.id"
                  @click="onComplete(row)"
                >
                  完成
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="panel-card">
          <div class="panel-card__head">
            <h2>我的排班</h2>
            <el-button link type="primary" @click="go('/staff/schedules')">排班详情</el-button>
          </div>
          <div class="week-row">
            <article
              v-for="day in weekSchedules"
              :key="day.date"
              class="day-card"
              :class="dayCardClass(day)"
            >
              <div class="day-card__date">{{ day.label }}</div>
              <div class="day-card__week">周{{ day.week }}</div>
              <div class="day-card__shift">{{ dayShiftLabel(day) }}</div>
              <div v-if="day.slots.length > 1" class="day-card__more">+{{ day.slots.length - 1 }}</div>
            </article>
          </div>
        </section>
      </section>

      <aside class="staff-home__right">
        <section class="side-card">
          <h3>今日提醒</h3>
          <ul class="tip-list">
            <li v-for="(item, idx) in reminderItems" :key="idx" :class="`tip-list--${item.tone}`">
              {{ item.text }}
            </li>
          </ul>
        </section>

        <section class="side-card">
          <h3>我的今日排班</h3>
          <div v-if="!todayShiftRows.length" class="empty-soft">今日暂无排班</div>
          <ul v-else class="shift-list">
            <li v-for="row in todayShiftRows" :key="row.id || `${row.startTime}-${row.endTime}`">
              <strong>{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</strong>
              <span>{{ row.status === 'AVAILABLE' ? '可用' : row.status || '排班' }}</span>
            </li>
          </ul>
        </section>

        <section class="side-card">
          <h3>常用功能</h3>
          <div class="quick-grid">
            <button
              v-for="item in quickEntries"
              :key="item.path"
              type="button"
              class="quick"
              :class="`quick--${item.tone}`"
              @click="go(item.path)"
            >
              <span class="quick__icon">
                <el-icon :size="18"><component :is="item.icon" /></el-icon>
              </span>
              <span>{{ item.title }}</span>
            </button>
          </div>
        </section>

        <section class="side-card">
          <div class="panel-card__head panel-card__head--compact">
            <h3>最新护理记录</h3>
            <el-button link type="primary" @click="go('/staff/service-records')">更多</el-button>
          </div>
          <div v-if="!latestRecords.length" class="empty-soft">暂无服务记录</div>
          <ul v-else class="record-list">
            <li v-for="row in latestRecords" :key="row.id">
              <div class="record-list__main">
                <strong>{{ row.elderName || '老人' }}</strong>
                <span>{{ row.serviceName || serviceTypeLabel(row.serviceType) }}</span>
              </div>
              <div class="record-list__meta">
                <em>{{ row.completedAt || row.actualEndTime || row.scheduledEndTime || '-' }}</em>
                <span class="status-tag status-tag--done">已完成</span>
              </div>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.staff-home {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.alert {
  border-radius: 12px;
}

.staff-home__top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--ec-right-panel, 300px);
  gap: 16px;
  align-items: start;
}

.staff-home__center {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.staff-home__right {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 8px;
}

/* Banner：中央卡片，固定高度对齐家属端 HeroCarousel，不撑整页 */
.hero-card {
  position: relative;
  height: 220px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(63, 120, 150, 0.1);
  background: #eaf7fc;
}

.hero-card__media {
  position: absolute;
  inset: 0;
  background-image: var(--hero-img);
  background-size: cover;
  background-position: center 42%;
}

.hero-card__date {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 1;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #244a65;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(36, 74, 101, 0.08);
}

.welcome-card,
.panel-card,
.side-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 18px rgba(63, 120, 150, 0.07);
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.welcome-card h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #244a65;
}

.welcome-card p {
  margin: 0;
  font-size: 13px;
  color: #6b8499;
}

.welcome-card__shift {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eaf7fc;
  color: #3f9eb9;
  font-size: 13px;
  font-weight: 600;
}

.attendance-card__body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.attendance-card__body p {
  margin: 0 0 4px;
  color: #4a6278;
  font-size: 13px;
}
.attendance-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-card__head h2,
.panel-card__head h3,
.side-card h3 {
  margin: 0;
  font-size: 16px;
  color: #244a65;
}

.panel-card__head span {
  font-size: 12px;
  color: #6b8499;
}

.panel-card__head--compact {
  margin-bottom: 10px;
}

.panel-card__head--compact h3,
.side-card h3 {
  font-size: 15px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: #f7fbfd;
}

.stat__icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.stat--blue .stat__icon {
  color: #3f9eb9;
  background: #eaf7fc;
}

.stat--orange .stat__icon {
  color: #e09a4a;
  background: #fff4dd;
}

.stat--teal .stat__icon {
  color: #4a90c2;
  background: #eaf0fb;
}

.stat--green .stat__icon {
  color: #58b79b;
  background: #eef9f3;
}

.stat p {
  margin: 0 0 2px;
  font-size: 12px;
  color: #6b8499;
}

.stat strong {
  font-size: 22px;
  color: #244a65;
  line-height: 1.2;
}

.type-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eaf7fc;
  color: #3f9eb9;
  font-size: 12px;
}

.status-tag {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag--pending {
  background: #fff4dd;
  color: #c8842a;
}

.status-tag--doing {
  background: #eaf7fc;
  color: #3f9eb9;
}

.status-tag--done {
  background: #eef9f3;
  color: #3f9a72;
}

.status-tag--muted {
  background: #f0f3f6;
  color: #8aa0b5;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.day-card {
  padding: 10px 6px;
  border-radius: 12px;
  text-align: center;
  background: #fff;
  border: 1px solid rgba(63, 158, 185, 0.1);
}

.day-card__date {
  font-size: 13px;
  font-weight: 700;
  color: #244a65;
}

.day-card__week {
  margin-top: 2px;
  font-size: 11px;
  color: #8aa0b5;
}

.day-card__shift {
  margin-top: 6px;
  font-size: 11px;
  color: #3f9eb9;
  font-weight: 600;
}

.day-card__more {
  margin-top: 2px;
  font-size: 10px;
  color: #8aa0b5;
}

.day-card.is-today {
  background: linear-gradient(160deg, #3f9eb9 0%, #58b79b 100%);
  border-color: transparent;
  box-shadow: 0 8px 16px rgba(63, 158, 185, 0.25);
}

.day-card.is-today .day-card__date,
.day-card.is-today .day-card__week,
.day-card.is-today .day-card__shift,
.day-card.is-today .day-card__more {
  color: #fff;
}

.day-card.is-rest {
  background: #f5f7f9;
}

.day-card.is-rest .day-card__shift {
  color: #8aa0b5;
  font-weight: 500;
}

.tip-list,
.shift-list,
.record-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-list li {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.tip-list--orange {
  background: #fff4dd;
  color: #c8842a;
}

.tip-list--blue {
  background: #eaf7fc;
  color: #3f9eb9;
}

.tip-list--green {
  background: #eef9f3;
  color: #3f9a72;
}

.tip-list--muted {
  background: #f5f7f9;
  color: #6b8499;
}

.shift-list li {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(63, 158, 185, 0.08);
  font-size: 13px;
}

.shift-list li:last-child {
  border-bottom: none;
}

.shift-list strong {
  color: #244a65;
}

.shift-list span {
  color: #58b79b;
  font-size: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.quick {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 8px;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  background: #f7fbfd;
  color: #244a65;
  font-size: 12px;
}

.quick__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
}

.quick--blue .quick__icon {
  background: #3f9eb9;
}

.quick--green .quick__icon {
  background: #66c89c;
}

.quick--teal .quick__icon {
  background: #58b79b;
}

.quick--orange .quick__icon {
  background: #e09a4a;
}

.quick:hover {
  background: #eaf7fc;
}

.empty-soft {
  padding: 16px 0;
  text-align: center;
  color: #8aa0b5;
  font-size: 13px;
}

.record-list li {
  padding: 10px 0;
  border-bottom: 1px solid rgba(63, 158, 185, 0.08);
}

.record-list li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.record-list__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-list__main strong {
  font-size: 13px;
  color: #244a65;
}

.record-list__main span {
  font-size: 12px;
  color: #6b8499;
}

.record-list__meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.record-list__meta em {
  font-style: normal;
  font-size: 11px;
  color: #8aa0b5;
}

@media (max-width: 1200px) {
  .staff-home__top {
    grid-template-columns: 1fr;
  }

  .staff-home__right {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .stat-grid {
    grid-template-columns: 1fr 1fr;
  }

  .week-row {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .staff-home__right {
    grid-template-columns: 1fr;
  }

  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-card {
    height: 180px;
  }
}

@media (max-width: 640px) {
  .stat-grid,
  .week-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

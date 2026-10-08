<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listMyElders } from '@/api/elder'
import {
  getHealthRecords,
  getHealthAlerts,
  toMeasuredParam,
} from '@/api/health'
import { unwrap } from '@/utils/familyHome'
import { healthPageBanner } from '@/config/familyImages'
import FamilyPageBanner from '@/components/family/FamilyPageBanner.vue'
import HealthElderPicker from '@/components/family/health/HealthElderPicker.vue'
import HealthMetricCards from '@/components/family/health/HealthMetricCards.vue'
import HealthTrendCharts from '@/components/family/health/HealthTrendCharts.vue'
import HealthRecordList from '@/components/family/health/HealthRecordList.vue'
import HealthAlertList from '@/components/family/health/HealthAlertList.vue'

const eldersLoading = ref(false)
const overviewLoading = ref(false)
const trendLoading = ref(false)
const recordsLoading = ref(false)
const alertsLoading = ref(false)

const elders = ref([])
const elderId = ref(null)
const accessError = ref('')

const latestRecord = ref(null)
const trendRecords = ref([])
const tableRecords = ref([])
const warnings = ref([])

const trendDays = ref(7)
const dateRange = ref(null)
const page = ref(1)
const pageSize = ref(10)
const alertStatusFilter = ref('ALL')

const currentElder = computed(() => elders.value.find((e) => e.id === elderId.value) || null)
const currentElderName = computed(() => currentElder.value?.name || '')

function daysAgo(days) {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - (days - 1))
  return d
}

function endOfToday() {
  const d = new Date()
  d.setHours(23, 59, 59, 999)
  return d
}

function handleAccessError(e) {
  if (e?.code === 403) {
    accessError.value = '您没有权限查看该老人的健康数据。'
    return true
  }
  if (e?.code === 404) {
    accessError.value = '健康数据不存在。'
    return true
  }
  if (e?.code === 500) {
    accessError.value = '系统服务异常，请稍后重试。'
    return true
  }
  return false
}

async function loadElders() {
  eldersLoading.value = true
  try {
    const data = unwrap(await listMyElders())
    elders.value = Array.isArray(data) ? data : []
    if (elders.value.length && !elderId.value) {
      elderId.value = elders.value[0].id
    }
    if (!elders.value.length) {
      elderId.value = null
    }
  } catch (e) {
    elders.value = []
    if (e.code !== 401) {
      ElMessage.error(e.message || '加载老人失败')
    }
  } finally {
    eldersLoading.value = false
  }
}

async function loadOverviewAndTrends() {
  if (!elderId.value) {
    latestRecord.value = null
    trendRecords.value = []
    return
  }

  overviewLoading.value = true
  trendLoading.value = true
  accessError.value = ''

  const trendFrom = toMeasuredParam(daysAgo(trendDays.value), false)
  const trendTo = toMeasuredParam(endOfToday(), true)

  try {
    const [latestRes, trendRes] = await Promise.all([
      getHealthRecords(elderId.value, { size: 1 }),
      getHealthRecords(elderId.value, {
        size: 100,
        measuredFrom: trendFrom,
        measuredTo: trendTo,
      }),
    ])
    const latestList = unwrap(latestRes)
    const trendList = unwrap(trendRes)
    latestRecord.value = Array.isArray(latestList) && latestList.length ? latestList[0] : null
    trendRecords.value = Array.isArray(trendList) ? trendList : []
  } catch (e) {
    latestRecord.value = null
    trendRecords.value = []
    if (!handleAccessError(e) && e.code !== 401) {
      ElMessage.error(e.message || '加载健康概览失败')
    }
  } finally {
    overviewLoading.value = false
    trendLoading.value = false
  }
}

async function loadRecords() {
  if (!elderId.value) {
    tableRecords.value = []
    return
  }

  recordsLoading.value = true
  const params = { size: 100 }
  if (dateRange.value?.length === 2) {
    params.measuredFrom = toMeasuredParam(dateRange.value[0], false)
    params.measuredTo = toMeasuredParam(dateRange.value[1], true)
  }

  try {
    const data = unwrap(await getHealthRecords(elderId.value, params))
    tableRecords.value = Array.isArray(data) ? data : []
    page.value = 1
  } catch (e) {
    tableRecords.value = []
    if (!handleAccessError(e) && e.code !== 401) {
      ElMessage.error(e.message || '加载健康记录失败')
    }
  } finally {
    recordsLoading.value = false
  }
}

async function loadWarnings() {
  if (!elderId.value) {
    warnings.value = []
    return
  }

  alertsLoading.value = true
  try {
    const data = unwrap(await getHealthAlerts(elderId.value))
    warnings.value = Array.isArray(data) ? data : []
  } catch (e) {
    warnings.value = []
    if (!handleAccessError(e) && e.code !== 401) {
      ElMessage.error(e.message || '加载健康预警失败')
    }
  } finally {
    alertsLoading.value = false
  }
}

async function refreshAll() {
  if (!elderId.value) {
    latestRecord.value = null
    trendRecords.value = []
    tableRecords.value = []
    warnings.value = []
    accessError.value = ''
    return
  }
  await Promise.all([loadOverviewAndTrends(), loadRecords(), loadWarnings()])
}

function onSearchRecords() {
  loadRecords()
}

function onResetRecords() {
  dateRange.value = null
  loadRecords()
}

watch(elderId, () => {
  refreshAll()
})

watch(trendDays, () => {
  loadOverviewAndTrends()
})

onMounted(async () => {
  await loadElders()
  await refreshAll()
})
</script>

<template>
  <div class="health-page">
    <header class="health-page__head">
      <div>
        <h1>健康管理</h1>
        <p>关注老人健康变化，让关爱更及时。</p>
      </div>
      <el-button :loading="overviewLoading || recordsLoading || alertsLoading" @click="refreshAll">
        刷新
      </el-button>
    </header>

    <FamilyPageBanner :banner="healthPageBanner" />

    <HealthElderPicker
      v-model="elderId"
      :elders="elders"
      :loading="eldersLoading"
    />

    <el-alert
      v-if="accessError"
      type="warning"
      :title="accessError"
      show-icon
      :closable="false"
      class="health-alert"
    />

    <el-empty
      v-if="!eldersLoading && !elders.length"
      description="暂未绑定老人"
    >
      <template #description>
        <p class="empty-title">暂未绑定老人</p>
        <p class="empty-desc">请联系管理员绑定老人档案后再查看健康数据。</p>
      </template>
    </el-empty>

    <template v-else-if="elderId">
      <HealthMetricCards :loading="overviewLoading" :record="latestRecord" />

      <HealthTrendCharts
        v-model:range-days="trendDays"
        :loading="trendLoading"
        :records="trendRecords"
      />

      <HealthAlertList
        v-model:status-filter="alertStatusFilter"
        :loading="alertsLoading"
        :warnings="warnings"
        :elder-name="currentElderName"
      />

      <HealthRecordList
        v-model:page="page"
        v-model:page-size="pageSize"
        v-model:date-range="dateRange"
        :loading="recordsLoading"
        :records="tableRecords"
        @search="onSearchRecords"
        @reset="onResetRecords"
      />
    </template>
  </div>
</template>

<style scoped>
.health-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.health-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.health-page__head h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2c4a5e;
}

.health-page__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.health-alert {
  border-radius: 12px;
}

.empty-title {
  margin: 0;
  color: #4a6072;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 6px 0 0;
  color: #8aa0b5;
  font-size: 12px;
}

</style>

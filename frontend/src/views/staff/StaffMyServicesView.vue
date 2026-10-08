<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  completeMyServiceOrder,
  pageMyServiceOrders,
  startMyServiceOrder,
  todayScheduleWindow,
} from '@/api/staff'
import { orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)
const actionLoadingId = ref(null)

const query = reactive({
  status: '',
  todayOnly: false,
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '待确认', value: 'PENDING' },
]

function durationText(m) {
  return m == null ? '-' : `${m} 分钟`
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的服务订单'
  return '暂无分配给我的服务订单'
}

function canStart(row) {
  return row?.status === 'CONFIRMED'
}

function canComplete(row) {
  return row?.status === 'IN_SERVICE'
}

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const params = {
      page: query.page,
      size: query.size,
      status: query.status || undefined,
    }
    if (query.todayOnly) {
      const win = todayScheduleWindow()
      params.scheduledStartFrom = win.scheduledStartFrom
      params.scheduledStartTo = win.scheduledStartTo
    }
    const res = await pageMyServiceOrders(params)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    rows.value = []
    total.value = 0
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function onSearch() {
  hasSearched.value = true
  query.page = 1
  loadList()
}

function onReset() {
  query.status = ''
  query.todayOnly = false
  query.page = 1
  hasSearched.value = false
  loadList()
}

function onPageChange(p) {
  query.page = p
  loadList()
}

function onSizeChange(s) {
  query.size = s
  query.page = 1
  loadList()
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
    await loadList()
  } catch {
    // 403/409 等已由 request.js 提示，避免双提示
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
    await loadList()
  } catch {
    // request.js 已提示
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(loadList)
</script>

<template>
  <div class="svc-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>我的服务</h2>
        <p>查看并执行分配给您的照护服务：已确认可开始，服务中可完成。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 140px">
            <el-option
              v-for="o in statusOptions"
              :key="o.value || 'all'"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="今日预约">
          <el-switch v-model="query.todayOnly" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">
        开始/完成走真实 POST /start、/complete；后端校验本人订单与状态流转。
      </p>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="err"
      :title="loadError"
    >
      <template #default>
        <el-button type="primary" link @click="loadList">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <el-table :data="rows" stripe :empty-text="emptyText()">
        <el-table-column prop="orderNo" label="订单编号" min-width="150">
          <template #default="{ row }">
            <code class="ono">{{ row.orderNo || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="elderName" label="老人" min-width="100">
          <template #default="{ row }">{{ row.elderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serviceName" label="服务项目" min-width="130">
          <template #default="{ row }">
            <div>{{ row.serviceName || '-' }}</div>
            <div class="sub">{{ serviceTypeLabel(row.serviceType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="180">
          <template #default="{ row }">
            <div>{{ row.scheduledStartTime || '-' }}</div>
            <div class="sub">至 {{ row.scheduledEndTime || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="90">
          <template #default="{ row }">{{ durationText(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusMeta(row.status).type" size="small">
              {{ orderStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="canStart(row)"
              link
              type="success"
              :loading="actionLoadingId === row.id"
              @click="onStart(row)"
            >
              开始服务
            </el-button>
            <el-button
              v-if="canComplete(row)"
              link
              type="warning"
              :loading="actionLoadingId === row.id"
              @click="onComplete(row)"
            >
              完成服务
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.svc-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #244a65;
}

.page-head p {
  margin: 0;
  color: #6b8499;
  font-size: 13px;
  line-height: 1.6;
  max-width: 640px;
}

.filter-card,
.table-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 18px rgba(63, 120, 150, 0.07);
}

.filter-hint {
  margin: 0;
  font-size: 12px;
  color: #6b8499;
}

.err {
  border-radius: 12px;
}

.ono {
  font-family: ui-monospace, Consolas, monospace;
  color: #3f9eb9;
}

.sub {
  margin-top: 2px;
  font-size: 12px;
  color: #6b8499;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

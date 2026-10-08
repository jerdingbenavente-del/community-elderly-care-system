<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageMyServiceRecords } from '@/api/staff'
import { orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
let loadSeq = 0

const query = reactive({
  dateRange: null,
  page: 1,
  size: 10,
})

function emptyText() {
  return '暂无服务记录'
}

function toIsoStart(day) {
  return day ? `${day}T00:00:00` : undefined
}

function toIsoEnd(day) {
  return day ? `${day}T23:59:59` : undefined
}

async function loadList() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const range = Array.isArray(query.dateRange) ? query.dateRange : null
    const from = range?.[0]
    const to = range?.[1]
    if (from && to && from > to) {
      rows.value = []
      total.value = 0
      loadError.value = '开始日期不能晚于结束日期'
      return
    }
    const res = await pageMyServiceRecords({
      page: query.page,
      size: query.size,
      scheduledStartFrom: toIsoStart(from),
      scheduledStartTo: toIsoEnd(to),
    })
    if (seq !== loadSeq) return
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    if (seq !== loadSeq) return
    rows.value = []
    total.value = 0
    loadError.value = e.message || '加载失败'
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  query.dateRange = null
  query.page = 1
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

onMounted(loadList)
</script>

<template>
  <div class="rec-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>服务记录</h2>
        <p>查看我已完成的照护服务（只读历史记录）。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">
        固定查询状态 COMPLETED；日期筛选作用在预约开始时间；详情复用「我的服务」订单详情。
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
        <el-table-column prop="completedAt" label="完成时间" min-width="160">
          <template #default="{ row }">{{ row.completedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusMeta(row.status).type" size="small">
              {{ orderStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
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
.rec-page {
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

@media (max-width: 720px) {
  .filter-card :deep(.el-form-item) {
    margin-right: 0;
  }
}
</style>

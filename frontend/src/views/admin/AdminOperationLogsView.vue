<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOperationLogs } from '@/api/operationLog'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailRow = ref(null)

const query = reactive({
  username: '',
  module: '',
  dateRange: null,
  page: 1,
  size: 10,
})

const moduleOptions = [
  { label: '全部模块', value: '' },
  { label: 'system', value: 'system' },
  { label: 'care', value: 'care' },
  { label: 'health', value: 'health' },
  { label: 'auth', value: 'auth' },
]

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function resultType(result) {
  if (!result) return 'info'
  const s = String(result).toUpperCase()
  if (s === 'SUCCESS' || s === 'OK') return 'success'
  if (s.includes('FAIL') || s.includes('ERROR') || s === 'DENIED') return 'danger'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      username: query.username?.trim() || undefined,
      module: query.module || undefined,
      dateFrom: query.dateRange?.[0] || undefined,
      dateTo: query.dateRange?.[1] || undefined,
    }
    const res = await pageOperationLogs(params)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(loadList)
</script>

<template>
  <div class="admin-logs-page">
    <header class="page-head">
      <div>
        <h1>操作日志</h1>
        <p>查询系统已记录的管理与业务操作，按时间倒序展示。</p>
      </div>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="query.username"
          clearable
          placeholder="操作用户"
          style="width: 160px"
          @keyup.enter="onSearch"
        />
        <el-select v-model="query.module" clearable placeholder="模块" style="width: 140px">
          <el-option
            v-for="o in moduleOptions"
            :key="o.value || 'all'"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          clearable
        />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无操作日志">
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作用户" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">{{ row.module || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operation" label="操作类型" min-width="150" show-overflow-tooltip />
        <el-table-column label="请求方式" width="90">
          <template #default="{ row }">{{ row.requestMethod || '-' }}</template>
        </el-table-column>
        <el-table-column label="请求路径" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.requestUri || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag :type="resultType(row.result)" size="small">{{ row.result || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadList"
          @size-change="() => { query.page = 1; loadList() }"
        />
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="日志详情" size="420px">
      <template v-if="detailRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作时间">{{ fmt(detailRow.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="操作用户">{{ detailRow.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ detailRow.userId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detailRow.module || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detailRow.operation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ detailRow.targetType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标 ID">{{ detailRow.targetId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ detailRow.requestMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求路径">{{ detailRow.requestUri || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求 IP">{{ detailRow.requestIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detailRow.result || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-logs-page { display: flex; flex-direction: column; gap: 16px; }
.page-head h1 { margin: 0; font-size: 22px; color: #23415f; }
.page-head p { margin: 6px 0 0; color: #7a94a8; font-size: 13px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 6px 18px rgba(36, 66, 92, 0.06); }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>

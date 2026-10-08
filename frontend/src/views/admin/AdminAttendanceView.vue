<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageAdminAttendance } from '@/api/careStaffAttendance'
import { pageCareStaff } from '@/api/careStaff'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const staffOptions = ref([])

const query = reactive({
  attendanceDate: '',
  careStaffId: null,
  status: '',
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '工作中', value: 'WORKING' },
  { label: '已完成', value: 'COMPLETED' },
]

function statusMeta(s) {
  if (s === 'WORKING') return { label: '工作中', type: 'warning' }
  if (s === 'COMPLETED') return { label: '已完成', type: 'success' }
  return { label: s || '-', type: 'info' }
}

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadStaff() {
  try {
    const res = await pageCareStaff({ page: 1, size: 100, status: 1 })
    staffOptions.value = res?.data?.records || []
  } catch {
    staffOptions.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await pageAdminAttendance({
      page: query.page,
      size: query.size,
      attendanceDate: query.attendanceDate || undefined,
      careStaffId: query.careStaffId || undefined,
      status: query.status || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载考勤失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStaff()
  await loadList()
})
</script>

<template>
  <div class="admin-attendance-page">
    <header class="page-head">
      <div>
        <h1>护理员考勤</h1>
        <p>查询全部护理员的上下班记录。本页只读，不补卡、不改时间。</p>
      </div>
    </header>
    <section class="panel">
      <div class="toolbar">
        <el-date-picker
          v-model="query.attendanceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="日期"
          clearable
        />
        <el-select v-model="query.careStaffId" clearable filterable placeholder="护理员" style="width: 180px">
          <el-option
            v-for="s in staffOptions"
            :key="s.id"
            :label="s.name || s.realName || s.username"
            :value="s.id"
          />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无考勤记录">
        <el-table-column label="护理员" min-width="120">
          <template #default="{ row }">{{ row.careStaffName || '-' }}</template>
        </el-table-column>
        <el-table-column label="工号" width="110">
          <template #default="{ row }">{{ row.employeeNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="attendanceDate" label="日期" width="130" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.checkInTime) }}</template>
        </el-table-column>
        <el-table-column label="签退时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.checkOutTime) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-attendance-page { display: flex; flex-direction: column; gap: 16px; }
.page-head h1 { margin: 0; font-size: 22px; color: #23415f; }
.page-head p { margin: 6px 0 0; color: #7a94a8; font-size: 13px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 6px 18px rgba(36, 66, 92, 0.06); }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>

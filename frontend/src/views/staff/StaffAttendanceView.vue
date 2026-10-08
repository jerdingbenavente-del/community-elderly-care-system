<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageMyAttendance } from '@/api/careStaffAttendance'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({
  range: [],
  page: 1,
  size: 10,
})

const statusMap = {
  WORKING: { label: '工作中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
}

function statusMeta(s) {
  return statusMap[s] || { label: s || '-', type: 'info' }
}

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadList() {
  loading.value = true
  try {
    const [startDate, endDate] = query.range || []
    const res = await pageMyAttendance({
      page: query.page,
      size: query.size,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载考勤记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="staff-attendance-page">
    <header class="page-head">
      <div>
        <h1>我的考勤</h1>
        <p>仅显示本人上下班记录，与服务订单执行签到相互独立。</p>
      </div>
    </header>
    <section class="panel">
      <div class="toolbar">
        <el-date-picker
          v-model="query.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无考勤记录">
        <el-table-column prop="attendanceDate" label="日期" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" min-width="180">
          <template #default="{ row }">{{ fmt(row.checkInTime) }}</template>
        </el-table-column>
        <el-table-column label="签退时间" min-width="180">
          <template #default="{ row }">{{ fmt(row.checkOutTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
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
.staff-attendance-page { display: flex; flex-direction: column; gap: 16px; }
.page-head h1 { margin: 0; font-size: 22px; color: #23415f; }
.page-head p { margin: 6px 0 0; color: #7a94a8; font-size: 13px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 6px 18px rgba(36, 66, 92, 0.06); }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>

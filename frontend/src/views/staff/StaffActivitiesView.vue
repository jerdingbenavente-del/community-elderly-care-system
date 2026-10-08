<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { pagePublicActivities } from '@/api/activity'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  status: '',
  dateFrom: '',
  dateTo: '',
  page: 1,
  size: 10,
})

function statusMeta(s) {
  if (s === 'PUBLISHED') return { label: '已发布', type: 'success' }
  if (s === 'CANCELLED') return { label: '已取消', type: 'warning' }
  if (s === 'COMPLETED') return { label: '已结束', type: 'info' }
  return { label: s || '-', type: 'info' }
}

function fmtTime(v) {
  if (!v) return '-'
  return String(v).slice(0, 5)
}

async function loadList() {
  loading.value = true
  try {
    const res = await pagePublicActivities({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      dateFrom: query.dateFrom || undefined,
      dateTo: query.dateTo || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载活动失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="staff-activity-page">
    <header class="page-head">
      <h1>社区活动</h1>
      <p>只读查看已发布及历史活动，不生成护理订单。</p>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="名称/地点" style="width: 180px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已结束" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-date-picker v-model="query.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker v-model="query.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无活动">
        <el-table-column prop="activityName" label="活动名称" min-width="140" />
        <el-table-column prop="activityDate" label="日期" width="120" />
        <el-table-column label="时间" width="130">
          <template #default="{ row }">{{ fmtTime(row.startTime) }}～{{ fmtTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0 0 14px; color: #667085; }
.panel { background: #fff; border-radius: 12px; padding: 16px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { formatLocalDate, pageMySchedules } from '@/api/staff'

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
let loadSeq = 0

const query = reactive({
  scheduleDate: formatLocalDate(),
  page: 1,
  size: 10,
})

function statusText(status) {
  if (status === 'AVAILABLE') return '可用'
  if (status === 'CANCELLED') return '已取消'
  return status || '-'
}

function statusTagType(status) {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'info'
}

function formatTime(t) {
  if (!t) return '-'
  // 后端可能返回 HH:mm:ss
  return String(t).slice(0, 5)
}

function formatTimeRange(row) {
  return `${formatTime(row.startTime)} ~ ${formatTime(row.endTime)}`
}

function emptyText() {
  return '暂无排班'
}

async function loadList() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageMySchedules({
      page: query.page,
      size: query.size,
      scheduleDate: query.scheduleDate || undefined,
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
  query.scheduleDate = formatLocalDate()
  query.page = 1
  loadList()
}

function onDateChange() {
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

onMounted(loadList)
</script>

<template>
  <div class="sch-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>我的排班</h2>
        <p>查看分配给您的可服务时段。排班由管理员维护，护理员端仅可查看。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="排班日期">
          <el-date-picker
            v-model="query.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :clearable="false"
            style="width: 180px"
            @change="onDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">今天</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">
        默认显示今天；数据来自真实排班接口，后端强制仅本人排班。
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
        <el-table-column prop="scheduleDate" label="排班日期" width="130">
          <template #default="{ row }">{{ row.scheduleDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="时段" min-width="160">
          <template #default="{ row }">{{ formatTimeRange(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="careStaffName" label="护理员" min-width="110">
          <template #default="{ row }">{{ row.careStaffName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark?.trim() || '（无）' }}</template>
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
.sch-page {
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

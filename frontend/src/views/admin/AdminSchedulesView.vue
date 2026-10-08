<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  pageCareStaffSchedules,
  updateCareStaffSchedule,
} from '@/api/careStaffSchedule'
import { pageCareStaff } from '@/api/careStaff'
import ScheduleFormDrawer from '@/components/admin/ScheduleFormDrawer.vue'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)
const actionLoadingId = ref(null)

const staffOptions = ref([])
const staffLoading = ref(false)

const query = reactive({
  careStaffId: null,
  status: '',
  startDate: '',
  endDate: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const editId = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '可用', value: 'AVAILABLE' },
  { label: '已取消', value: 'CANCELLED' },
]

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

function formatTimeRange(row) {
  const s = row.startTime || '-'
  const e = row.endTime || '-'
  return `${s} ~ ${e}`
}

async function loadStaff() {
  staffLoading.value = true
  try {
    const res = await pageCareStaff({ page: 1, size: 100, status: 1 })
    if (res?.code !== 200) throw new Error(res?.message || '加载护理员失败')
    staffOptions.value = (res.data?.records || []).map((s) => ({
      id: s.id,
      label: `${s.name || '-'}${s.employeeNo ? `（${s.employeeNo}）` : ''}`,
    }))
  } catch {
    staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageCareStaffSchedules({
      page: query.page,
      size: query.size,
      careStaffId: query.careStaffId || undefined,
      status: query.status || undefined,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined,
    })
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
  query.careStaffId = null
  query.status = ''
  query.startDate = ''
  query.endDate = ''
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

function openCreate() {
  editId.value = null
  formVisible.value = true
}

function openEdit(row) {
  editId.value = row.id
  formVisible.value = true
}

function goDetail(row) {
  router.push(`/admin/schedules/${row.id}`)
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的排班'
  return '暂无排班记录'
}

async function cancelSchedule(row) {
  if (row.status === 'CANCELLED') return
  try {
    await ElMessageBox.confirm(
      '取消后该时段将无法用于订单确认分配，是否确认取消？',
      '取消排班',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  actionLoadingId.value = row.id
  try {
    const res = await updateCareStaffSchedule(row.id, {
      scheduleDate: row.scheduleDate,
      startTime: row.startTime,
      endTime: row.endTime,
      status: 'CANCELLED',
      remark: row.remark || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('排班已取消')
    await loadList()
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '取消失败')
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(() => {
  loadStaff()
  loadList()
})
</script>

<template>
  <div class="sched-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>排班管理</h2>
        <p>管理护理员可服务时段，供服务订单确认分配时校验覆盖。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增排班</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="护理员">
          <el-select
            v-model="query.careStaffId"
            clearable
            filterable
            :loading="staffLoading"
            placeholder="全部"
            style="width: 180px"
          >
            <el-option
              v-for="o in staffOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 120px">
            <el-option
              v-for="o in statusOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期起">
          <el-date-picker
            v-model="query.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="日期止">
          <el-date-picker
            v-model="query.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
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
        <el-table-column prop="careStaffName" label="护理员" min-width="120">
          <template #default="{ row }">{{ row.careStaffName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="scheduleDate" label="排班日期" width="120" />
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
        <el-table-column prop="remark" label="备注" min-width="140">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'AVAILABLE'"
              link
              type="warning"
              :loading="actionLoadingId === row.id"
              @click="cancelSchedule(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!rows.length && !loadError && !loading" class="empty-action">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增排班</el-button>
      </div>

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

    <ScheduleFormDrawer v-model="formVisible" :schedule-id="editId" @success="loadList" />
  </div>
</template>

<style scoped>
.sched-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  max-width: 640px;
}

.filter-card,
.table-card {
  padding: 16px 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.err {
  border-radius: 12px;
}

.empty-action {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

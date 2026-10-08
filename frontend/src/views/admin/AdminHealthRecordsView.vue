<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageHealthRecords, toMeasuredParam } from '@/api/health'
import { pageElders } from '@/api/adminElder'
import { formatDateTime } from '@/utils/familyHome'
import HealthRecordFormDrawer from '@/components/admin/HealthRecordFormDrawer.vue'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)
let loadSeq = 0

const elderOptions = ref([])
const elderLoading = ref(false)

const query = reactive({
  elderId: null,
  dateRange: null,
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const editId = ref(null)

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的健康记录'
  return '暂无健康记录'
}

function bpText(row) {
  if (row.systolicPressure == null && row.diastolicPressure == null) return '-'
  const s = row.systolicPressure ?? '-'
  const d = row.diastolicPressure ?? '-'
  return `${s} / ${d}`
}

async function loadElders(keyword) {
  elderLoading.value = true
  try {
    const res = await pageElders({
      page: 1,
      size: 20,
      status: 1,
      name: keyword?.trim() || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载老人失败')
    elderOptions.value = (res.data?.records || []).map((e) => ({
      id: e.id,
      label: e.name || `老人#${e.id}`,
    }))
  } catch {
    elderOptions.value = []
  } finally {
    elderLoading.value = false
  }
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
    const res = await pageHealthRecords({
      page: query.page,
      size: query.size,
      elderId: query.elderId || undefined,
      measuredFrom: toMeasuredParam(from, false),
      measuredTo: toMeasuredParam(to, true),
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
  hasSearched.value = true
  query.page = 1
  loadList()
}

function onReset() {
  query.elderId = null
  query.dateRange = null
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
  router.push(`/admin/health/${row.id}`)
}

onMounted(() => {
  loadElders()
  loadList()
})
</script>

<template>
  <div class="hr-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>健康档案</h2>
        <p>查看与录入老人健康测量记录。异常指标会按系统阈值自动生成预警。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增记录</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="老人">
          <el-select
            v-model="query.elderId"
            clearable
            filterable
            remote
            :remote-method="loadElders"
            :loading="elderLoading"
            placeholder="全部老人"
            style="width: 180px"
          >
            <el-option
              v-for="o in elderOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="测量日期">
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
      <p class="filter-hint">复用 P2 接口 GET /api/health-records；按老人 ID 与测量时间筛选。</p>
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
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="elderName" label="老人" min-width="110">
          <template #default="{ row }">{{ row.elderName || '-' }}</template>
        </el-table-column>
        <el-table-column label="测量时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.measuredAt) }}</template>
        </el-table-column>
        <el-table-column label="血压" width="120">
          <template #default="{ row }">{{ bpText(row) }}</template>
        </el-table-column>
        <el-table-column label="血糖" width="90">
          <template #default="{ row }">{{ row.bloodGlucose ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="体温" width="90">
          <template #default="{ row }">{{ row.bodyTemperature ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="心率" width="80">
          <template #default="{ row }">{{ row.heartRate ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark?.trim() || '（无）' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <HealthRecordFormDrawer
      v-model="formVisible"
      :record-id="editId"
      @success="loadList"
    />
  </div>
</template>

<style scoped>
.hr-page {
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

.filter-hint {
  margin: 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
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
  .page-head {
    flex-direction: column;
  }
}
</style>

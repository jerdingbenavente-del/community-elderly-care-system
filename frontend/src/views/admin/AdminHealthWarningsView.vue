<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageHealthWarnings } from '@/api/healthWarning'
import { pageElders } from '@/api/adminElder'
import {
  directionLabel,
  formatDateTime,
  indicatorLabel,
  warningLevelMeta,
  warningStatusMeta,
} from '@/utils/familyHome'
import HandleWarningDrawer from '@/components/admin/HandleWarningDrawer.vue'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)

const elderOptions = ref([])
const elderLoading = ref(false)

const query = reactive({
  status: '',
  indicator: '',
  elderId: null,
  page: 1,
  size: 10,
})

const handleVisible = ref(false)
const handleTarget = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'UNHANDLED' },
  { label: '已处理', value: 'HANDLED' },
]

const indicatorOptions = [
  { label: '全部指标', value: '' },
  { label: '收缩压', value: 'SYSTOLIC_PRESSURE' },
  { label: '舒张压', value: 'DIASTOLIC_PRESSURE' },
  { label: '血糖', value: 'BLOOD_GLUCOSE' },
  { label: '体温', value: 'TEMPERATURE' },
  { label: '心率', value: 'HEART_RATE' },
]

function alertTitle(row) {
  const name = indicatorLabel(row.indicator)
  const dir = directionLabel(row.direction)
  return dir ? `${name}异常（${dir}）` : `${name}异常`
}

function levelTagType(level) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'WARNING') return 'warning'
  return 'info'
}

function canHandle(row) {
  return row.status === 'UNHANDLED'
}

async function loadElders() {
  elderLoading.value = true
  try {
    const res = await pageElders({ page: 1, size: 100, status: 1 })
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
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageHealthWarnings({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      indicator: query.indicator || undefined,
      elderId: query.elderId || undefined,
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
  query.status = ''
  query.indicator = ''
  query.elderId = null
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
  router.push(`/admin/warnings/${row.id}`)
}

function goHealthRecord(row) {
  if (!row?.healthRecordId) return
  router.push(`/admin/health/${row.healthRecordId}`)
}

function openHandle(row) {
  handleTarget.value = row
  handleVisible.value = true
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的预警'
  return '暂无健康预警'
}

onMounted(() => {
  loadElders()
  loadList()
})
</script>

<template>
  <div class="warn-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>健康预警管理</h2>
        <p>及时处理老人的异常健康数据预警（辅助提醒，非医疗诊断）。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 130px">
            <el-option
              v-for="o in statusOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="query.indicator" style="width: 140px">
            <el-option
              v-for="o in indicatorOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="老人">
          <el-select
            v-model="query.elderId"
            clearable
            filterable
            :loading="elderLoading"
            placeholder="全部老人"
            style="width: 170px"
          >
            <el-option
              v-for="o in elderOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">后端支持按老人 ID、状态、指标筛选；不支持姓名模糊搜索。</p>
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
        <el-table-column prop="elderName" label="老人" min-width="100">
          <template #default="{ row }">{{ row.elderName || '-' }}</template>
        </el-table-column>
        <el-table-column label="健康记录ID" width="110">
          <template #default="{ row }">
            <code v-if="row.healthRecordId != null" class="rid">{{ row.healthRecordId }}</code>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="预警类型" min-width="150">
          <template #default="{ row }">{{ alertTitle(row) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.warningLevel)" size="small">
              {{ warningLevelMeta(row.warningLevel).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实测值" width="100">
          <template #default="{ row }">{{ row.actualValue ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="阈值说明" min-width="140">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.thresholdDesc"
              :content="row.thresholdDesc"
              placement="top"
              :show-after="400"
            >
              <span class="ellipsis">{{ row.thresholdDesc }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="warningStatusMeta(row.status).type" size="small">
              {{ warningStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.generatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="row.healthRecordId != null"
              link
              type="primary"
              @click="goHealthRecord(row)"
            >
              查看健康档案
            </el-button>
            <el-button
              v-if="canHandle(row)"
              link
              type="warning"
              @click="openHandle(row)"
            >
              处理
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

    <HandleWarningDrawer
      v-model="handleVisible"
      :warning="handleTarget"
      @success="loadList"
    />
  </div>
</template>

<style scoped>
.warn-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
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

.rid {
  font-family: ui-monospace, Consolas, monospace;
  color: #357a68;
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

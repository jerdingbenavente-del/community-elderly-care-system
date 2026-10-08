<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageAdminEvaluations } from '@/api/careEvaluation'
import { pageElders } from '@/api/adminElder'
import { formatDateTime } from '@/utils/familyHome'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)

const elderOptions = ref([])
const elderLoading = ref(false)

const query = reactive({
  elderId: null,
  elderName: '',
  serviceOrderId: '',
  score: '',
  page: 1,
  size: 10,
})

const scoreOptions = [
  { label: '全部评分', value: '' },
  { label: '5 分', value: 5 },
  { label: '4 分', value: 4 },
  { label: '3 分', value: 3 },
  { label: '2 分', value: 2 },
  { label: '1 分', value: 1 },
]

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
    const orderIdRaw = String(query.serviceOrderId || '').trim()
    const params = {
      page: query.page,
      size: query.size,
      elderId: query.elderId || undefined,
      elderName: query.elderName?.trim() || undefined,
      score: query.score === '' ? undefined : query.score,
    }
    if (orderIdRaw && /^\d+$/.test(orderIdRaw)) {
      params.serviceOrderId = Number(orderIdRaw)
    }
    const res = await pageAdminEvaluations(params)
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
  query.elderId = null
  query.elderName = ''
  query.serviceOrderId = ''
  query.score = ''
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
  router.push(`/admin/evaluations/${row.id}`)
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的评价'
  return '暂无服务评价'
}

onMounted(() => {
  loadElders()
  loadList()
})
</script>

<template>
  <div class="eval-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>服务评价管理</h2>
        <p>查看家属对养老照护服务的评价（只读，评分 1–5）。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="老人姓名">
          <el-input
            v-model="query.elderName"
            clearable
            placeholder="请输入老人姓名"
            style="width: 160px"
            @keyup.enter="onSearch"
          />
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
        <el-form-item label="订单 ID">
          <el-input
            v-model="query.serviceOrderId"
            clearable
            placeholder="精确订单 ID"
            style="width: 140px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.score" style="width: 120px">
            <el-option
              v-for="o in scoreOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">
        支持老人姓名模糊、elderId / serviceOrderId / score；护理员来自关联订单批量组装。
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
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="elderName" label="老人" min-width="100">
          <template #default="{ row }">{{ row.elderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serviceName" label="服务项目" min-width="130">
          <template #default="{ row }">{{ row.serviceName || '-' }}</template>
        </el-table-column>
        <el-table-column label="护理员" min-width="100">
          <template #default="{ row }">
            <span v-if="row.careStaffName">{{ row.careStaffName }}</span>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号" min-width="150">
          <template #default="{ row }">
            <code class="ono">{{ row.orderNo || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <div class="score-cell">
              <el-rate :model-value="Number(row.score) || 0" disabled :max="5" />
              <span class="score-num">{{ row.score ?? '-' }} / 5</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="180">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.content"
              :content="row.content"
              placement="top"
              :show-after="400"
            >
              <span class="ellipsis">{{ row.content }}</span>
            </el-tooltip>
            <span v-else class="muted">（未填写）</span>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
.eval-page {
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

.ono {
  font-family: ui-monospace, Consolas, monospace;
  color: var(--ec-color-primary-dark, #3a7a9c);
}

.score-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.score-num {
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.muted {
  color: var(--ec-text-secondary);
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

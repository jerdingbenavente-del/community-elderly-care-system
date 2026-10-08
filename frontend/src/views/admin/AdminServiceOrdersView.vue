<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  cancelServiceOrder,
  listServiceOrders,
} from '@/api/serviceOrder'
import { pageElders } from '@/api/adminElder'
import { orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'
import OrderConfirmAssignDrawer from '@/components/admin/OrderConfirmAssignDrawer.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)
const actionLoadingId = ref(null)

const elderOptions = ref([])
const elderLoading = ref(false)

const query = reactive({
  status: '',
  elderId: null,
  page: 1,
  size: 10,
})

const confirmVisible = ref(false)
const confirmOrder = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

function durationText(m) {
  return m == null ? '-' : `${m} 分钟`
}

function canConfirm(row) {
  return row.status === 'PENDING'
}

function canCancel(row) {
  return row.status === 'PENDING' || row.status === 'CONFIRMED'
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
    const res = await listServiceOrders({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
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
  router.push(`/admin/orders/${row.id}`)
}

function openConfirm(row) {
  confirmOrder.value = row
  confirmVisible.value = true
}

async function onCancel(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（可选）', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因，最多 500 字',
      inputValue: '',
      type: 'warning',
      inputValidator: (v) => {
        if (v && String(v).length > 500) return '取消原因不能超过 500 字'
        return true
      },
    })
    actionLoadingId.value = row.id
    const body = {}
    if (value?.trim()) body.cancelReason = value.trim()
    const res = await cancelServiceOrder(row.id, body)
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('订单已取消')
    await loadList()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    // request.js 已提示
  } finally {
    actionLoadingId.value = null
  }
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的订单'
  return '暂无服务订单'
}

onMounted(() => {
  const qs = typeof route.query.status === 'string' ? route.query.status : ''
  if (qs) query.status = qs
  loadElders()
  loadList()
})
</script>

<template>
  <div class="orders-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>服务订单管理</h2>
        <p>管理老人照护服务预约、订单状态和护理员分配。</p>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 140px">
            <el-option
              v-for="o in statusOptions"
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
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="filter-hint">
        后端查询支持状态、老人 ID 等；不支持按订单号/姓名模糊搜索（未做前端假筛选）。
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
        <el-table-column prop="orderNo" label="订单编号" min-width="160">
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
        <el-table-column label="时长" width="90">
          <template #default="{ row }">{{ durationText(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column label="护理员" min-width="120">
          <template #default="{ row }">
            <span v-if="row.careStaffName">{{ row.careStaffName }}</span>
            <span v-else class="muted">
              {{ row.status === 'PENDING' ? '待重新安排' : '未分配' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusMeta(row.status).type" size="small">
              {{ orderStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            {{ row.amount != null ? `¥${Number(row.amount).toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="支付" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'">
              {{ row.paymentStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="canConfirm(row)"
              link
              type="success"
              @click="openConfirm(row)"
            >
              确认分配
            </el-button>
            <el-button
              v-if="canCancel(row)"
              link
              type="warning"
              :loading="actionLoadingId === row.id"
              @click="onCancel(row)"
            >
              取消
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

    <OrderConfirmAssignDrawer
      v-model="confirmVisible"
      :order="confirmOrder"
      @success="loadList"
    />
  </div>
</template>

<style scoped>
.orders-page {
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

.sub {
  font-size: 12px;
  color: var(--ec-text-secondary);
  margin-top: 2px;
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

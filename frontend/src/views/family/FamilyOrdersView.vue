<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyElders } from '@/api/elder'
import { listMyEvaluations } from '@/api/evaluation'
import {
  listServiceOrders,
  getServiceOrder,
  cancelServiceOrder,
} from '@/api/serviceOrder'
import { unwrap, orderStatusMeta } from '@/utils/familyHome'
import { ordersPageBanner } from '@/config/familyImages'
import FamilyPageBanner from '@/components/family/FamilyPageBanner.vue'
import OrderCard from '@/components/family/order/OrderCard.vue'
import OrderDetailDrawer from '@/components/family/order/OrderDetailDrawer.vue'
import EvaluationFormDialog from '@/components/family/evaluation/EvaluationFormDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const cancelling = ref(false)
const eldersLoading = ref(false)

const elders = ref([])
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const elderFilter = ref(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref(null)

/** serviceOrderId -> evaluation */
const evaluationMap = ref(new Map())

const evalVisible = ref(false)
const evalMode = ref('create')
const evalOrder = ref(null)
const evalRecord = ref(null)

function isEvaluated(orderId) {
  return evaluationMap.value.has(orderId)
}

function evaluationOf(orderId) {
  return evaluationMap.value.get(orderId) || null
}

const statusTabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待确认' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'IN_SERVICE', label: '服务中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const highlightId = computed(() => {
  const raw = route.query.id
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
})

async function loadElders() {
  eldersLoading.value = true
  try {
    const data = unwrap(await listMyElders())
    elders.value = Array.isArray(data) ? data : []
  } catch {
    elders.value = []
  } finally {
    eldersLoading.value = false
  }
}

async function loadEvaluations() {
  try {
    const data = unwrap(await listMyEvaluations())
    const map = new Map()
    if (Array.isArray(data)) {
      data.forEach((e) => {
        if (e?.serviceOrderId != null) map.set(e.serviceOrderId, e)
      })
    }
    evaluationMap.value = map
  } catch {
    evaluationMap.value = new Map()
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
    }
    if (statusFilter.value) params.status = statusFilter.value
    if (elderFilter.value) params.elderId = elderFilter.value

    const data = unwrap(await listServiceOrders(params))
    orders.value = Array.isArray(data?.records) ? data.records : []
    total.value = Number(data?.total) || 0
  } catch (e) {
    orders.value = []
    total.value = 0
    if (e.code === 403) {
      ElMessage.warning('您没有权限查看订单。')
    } else if (e.code !== 401) {
      ElMessage.error(e.message || '加载订单失败')
    }
  } finally {
    loading.value = false
  }
}

async function openDetail(order) {
  detailVisible.value = true
  detailLoading.value = true
  detailOrder.value = order
  try {
    detailOrder.value = unwrap(await getServiceOrder(order.id))
  } catch (e) {
    if (e.code === 403) {
      ElMessage.warning('您没有权限查看该订单。')
      detailVisible.value = false
    } else if (e.code === 404) {
      ElMessage.warning('订单不存在。')
      detailVisible.value = false
    } else if (e.code !== 401) {
      ElMessage.error(e.message || '加载详情失败')
    }
  } finally {
    detailLoading.value = false
  }
}

async function onCancel(order) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（可选）', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因',
      inputValue: '',
      inputValidator: (v) => {
        if (v && String(v).length > 500) return '取消原因不能超过 500 字'
        return true
      },
    })
    cancelling.value = true
    const body = {}
    if (value?.trim()) body.cancelReason = value.trim()
    unwrap(await cancelServiceOrder(order.id, body))
    ElMessage.success('已取消预约')
    detailVisible.value = false
    await loadOrders()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    // 403/409 等已由 request.js 统一提示
  } finally {
    cancelling.value = false
  }
}

function onStatusChange(val) {
  statusFilter.value = val
  page.value = 1
  loadOrders()
}

function onElderChange() {
  page.value = 1
  loadOrders()
}

function onPageChange(p) {
  page.value = p
  loadOrders()
}

function onSizeChange(s) {
  pageSize.value = s
  page.value = 1
  loadOrders()
}

function goBook() {
  router.push('/family/care-services')
}

function openEvaluate(order) {
  evalMode.value = 'create'
  evalOrder.value = order
  evalRecord.value = null
  evalVisible.value = true
}

function openViewEvaluation(order) {
  evalMode.value = 'view'
  evalOrder.value = order
  evalRecord.value = evaluationOf(order.id)
  evalVisible.value = true
}

function onEvalSuccess(data) {
  if (data?.serviceOrderId != null) {
    const next = new Map(evaluationMap.value)
    next.set(data.serviceOrderId, data)
    evaluationMap.value = next
  } else {
    loadEvaluations()
  }
}

watch(highlightId, async (id) => {
  if (!id || loading.value) return
  const found = orders.value.find((o) => o.id === id)
  if (found) openDetail(found)
})

onMounted(async () => {
  // 支持从评价页带回 ?status=COMPLETED
  const statusQ = route.query.status
  if (typeof statusQ === 'string' && statusTabs.some((t) => t.value === statusQ)) {
    statusFilter.value = statusQ
  }
  await Promise.all([loadElders(), loadOrders(), loadEvaluations()])
  if (highlightId.value) {
    const found = orders.value.find((o) => o.id === highlightId.value)
    if (found) openDetail(found)
    else {
      try {
        const order = unwrap(await getServiceOrder(highlightId.value))
        openDetail(order)
      } catch {
        // ignore
      }
    }
  }
})
</script>

<template>
  <div class="orders-page">
    <header class="orders-page__head">
      <div>
        <h1>我的订单</h1>
        <p>查看服务预约进度，及时掌握照护安排。</p>
      </div>
      <div class="orders-page__actions">
        <el-button @click="goBook">去预约服务</el-button>
        <el-button :loading="loading" type="primary" @click="loadOrders">刷新</el-button>
      </div>
    </header>

    <FamilyPageBanner :banner="ordersPageBanner" />

    <section class="filter-panel">
      <div class="filter-panel__tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value || 'all'"
          type="button"
          class="status-chip"
          :class="{ 'is-active': statusFilter === tab.value }"
          @click="onStatusChange(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <el-select
        v-model="elderFilter"
        clearable
        placeholder="全部老人"
        style="width: 160px"
        :loading="eldersLoading"
        @change="onElderChange"
      >
        <el-option
          v-for="e in elders"
          :key="e.id"
          :label="e.name"
          :value="e.id"
        />
      </el-select>
    </section>

    <el-skeleton v-if="loading" class="panel" :rows="6" animated />

    <div v-else-if="!orders.length" class="orders-empty">
      <el-empty :image-size="88">
        <template #description>
          <p class="empty-title">暂无服务订单</p>
          <p class="empty-desc">
            {{
              statusFilter || elderFilter
                ? '当前筛选条件下没有订单，可切换状态或老人后再试。'
                : '您还没有服务预约，去照护服务页为老人预约吧。'
            }}
          </p>
        </template>
        <el-button type="primary" @click="goBook">去预约服务</el-button>
      </el-empty>
    </div>

    <template v-else>
      <div class="orders-grid">
        <OrderCard
          v-for="order in orders"
          :key="order.id"
          :order="order"
          :cancelling="cancelling"
          :evaluated="isEvaluated(order.id)"
          @detail="openDetail"
          @cancel="onCancel"
          @evaluate="openEvaluate"
          @view-evaluation="openViewEvaluation"
        />
      </div>
      <div class="orders-pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
      <p class="orders-hint">
        状态说明：
        <template v-for="(tab, idx) in statusTabs.slice(1)" :key="tab.value">
          {{ orderStatusMeta(tab.value).label }}{{ idx < statusTabs.length - 2 ? ' · ' : '' }}
        </template>
        。家属可取消「待确认 / 已确认」订单；「已完成」订单可评价（每个订单一次）。
      </p>
    </template>

    <OrderDetailDrawer
      v-model:visible="detailVisible"
      :loading="detailLoading"
      :order="detailOrder"
      :cancelling="cancelling"
      :evaluated="detailOrder ? isEvaluated(detailOrder.id) : false"
      @cancel="onCancel"
      @evaluate="openEvaluate"
      @view-evaluation="openViewEvaluation"
    />

    <EvaluationFormDialog
      v-model:visible="evalVisible"
      :mode="evalMode"
      :order="evalOrder"
      :evaluation="evalRecord"
      @success="onEvalSuccess"
    />
  </div>
</template>

<style scoped>
.orders-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.orders-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.orders-page__head h1 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.orders-page__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.orders-page__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.filter-panel__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-chip {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(74, 144, 194, 0.16);
  background: rgba(234, 244, 251, 0.55);
  color: #4a6072;
  font-size: 13px;
  cursor: pointer;
}

.status-chip.is-active {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(90deg, #4a90c2, #5ca4d6);
  font-weight: 600;
}

.panel {
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  padding: 16px;
}

.orders-empty {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.empty-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a6072;
}

.empty-desc {
  margin: 6px 0 12px;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.orders-pager {
  display: flex;
  justify-content: flex-end;
}

.orders-hint {
  margin: 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .orders-grid {
    grid-template-columns: 1fr;
  }
}
</style>

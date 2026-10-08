<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyEvaluations } from '@/api/evaluation'
import { getServiceOrder, listServiceOrders } from '@/api/serviceOrder'
import { unwrap, formatDateTime } from '@/utils/familyHome'
import { evaluationsPageBanner } from '@/config/familyImages'
import FamilyPageBanner from '@/components/family/FamilyPageBanner.vue'
import EvaluationCard from '@/components/family/evaluation/EvaluationCard.vue'
import EvaluationFormDialog from '@/components/family/evaluation/EvaluationFormDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const pendingLoading = ref(false)
const list = ref([])
const pendingOrders = ref([])

const formVisible = ref(false)
const formMode = ref('create')
const formOrder = ref(null)
const formEvaluation = ref(null)

const avgScore = computed(() => {
  if (!list.value.length) return null
  const sum = list.value.reduce((acc, e) => acc + (Number(e.score) || 0), 0)
  return (sum / list.value.length).toFixed(1)
})

const highlightId = computed(() => {
  const raw = route.query.id
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
})

const pendingOrderId = computed(() => {
  const raw = route.query.orderId
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
})

async function load() {
  loading.value = true
  try {
    const data = unwrap(await listMyEvaluations())
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
    if (e.code === 403) {
      // interceptor warned
    } else if (e.code !== 401) {
      ElMessage.error(e.message || '加载评价失败')
    }
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const data = unwrap(await listServiceOrders({ page: 1, size: 100, status: 'COMPLETED' }))
    const records = Array.isArray(data?.records) ? data.records : []
    const evaluatedIds = new Set(list.value.map((e) => e.serviceOrderId))
    pendingOrders.value = records.filter((o) => !evaluatedIds.has(o.id))
  } catch {
    pendingOrders.value = []
  } finally {
    pendingLoading.value = false
  }
}

async function refreshAll() {
  await load()
  await loadPending()
}

function openView(evaluation) {
  formMode.value = 'view'
  formEvaluation.value = evaluation
  formOrder.value = null
  formVisible.value = true
}

async function openCreateForOrder(orderId) {
  try {
    const existing = list.value.find((e) => e.serviceOrderId === orderId)
    if (existing) {
      openView(existing)
      return
    }

    const order = unwrap(await getServiceOrder(orderId))
    if (order.status !== 'COMPLETED') {
      ElMessage.warning('只有已完成的服务订单可以评价')
      return
    }
    formMode.value = 'create'
    formOrder.value = order
    formEvaluation.value = null
    formVisible.value = true
  } catch (e) {
    if (e.code === 403) {
      // interceptor warned
    } else if (e.code === 404) {
      ElMessage.warning('订单不存在。')
    } else if (e.code !== 401) {
      ElMessage.error(e.message || '无法打开评价')
    }
  }
}

async function findUnevaluatedCompleted() {
  try {
    const data = unwrap(
      await listServiceOrders({ page: 1, size: 20, status: 'COMPLETED' }),
    )
    const records = Array.isArray(data?.records) ? data.records : []
    const evaluatedIds = new Set(list.value.map((e) => e.serviceOrderId))
    return records.find((o) => !evaluatedIds.has(o.id)) || null
  } catch {
    return null
  }
}

async function goEvaluateFromEmpty() {
  const order = await findUnevaluatedCompleted()
  if (order) {
    openCreateForOrder(order.id)
    return
  }
  router.push({ path: '/family/orders', query: { status: 'COMPLETED' } })
}

function onFormSuccess(evaluation) {
  if (evaluation) {
    const idx = list.value.findIndex((e) => e.id === evaluation.id)
    if (idx >= 0) list.value.splice(idx, 1, evaluation)
    else list.value.unshift(evaluation)
  } else {
    load()
  }
  loadPending()
  if (route.query.orderId) {
    router.replace({ path: '/family/evaluations' })
  }
}

watch(highlightId, (id) => {
  if (!id || loading.value) return
  const found = list.value.find((e) => e.id === id || e.serviceOrderId === id)
  if (found) openView(found)
})

onMounted(async () => {
  await refreshAll()
  if (pendingOrderId.value) {
    await openCreateForOrder(pendingOrderId.value)
  } else if (highlightId.value) {
    const found = list.value.find(
      (e) => e.id === highlightId.value || e.serviceOrderId === highlightId.value,
    )
    if (found) openView(found)
  }
})
</script>

<template>
  <div class="eval-page">
    <header class="eval-page__head">
      <div>
        <h1>服务评价</h1>
        <p>为已完成的照护服务打分留言，帮助我们持续改进。</p>
      </div>
      <div class="eval-page__actions">
        <el-button @click="router.push('/family/orders')">我的订单</el-button>
        <el-button :loading="loading || pendingLoading" type="primary" @click="refreshAll">
          刷新
        </el-button>
      </div>
    </header>

    <FamilyPageBanner :banner="evaluationsPageBanner" />

    <section class="stat-panel">
      <div class="stat-card">
        <span>已提交评价</span>
        <strong>{{ list.length }}</strong>
      </div>
      <div class="stat-card">
        <span>平均评分</span>
        <strong>{{ avgScore != null ? `${avgScore} 分` : '-' }}</strong>
      </div>
      <div class="stat-card">
        <span>待评价订单</span>
        <strong>{{ pendingOrders.length }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <div>
          <h3>待评价服务</h3>
          <p>仅已完成订单可评价，每个订单只能评价一次</p>
        </div>
      </div>
      <el-skeleton v-if="pendingLoading" :rows="3" animated />
      <el-empty
        v-else-if="!pendingOrders.length"
        description="暂无待评价订单"
        :image-size="64"
      />
      <div v-else class="pending-list">
        <div v-for="order in pendingOrders" :key="order.id" class="pending-item">
          <div>
            <strong>{{ order.serviceName }}</strong>
            <p>
              {{ order.elderName || '-' }} · 订单号 {{ order.orderNo || '-' }}
              <template v-if="order.completedAt">
                · 完成于 {{ formatDateTime(order.completedAt) }}
              </template>
            </p>
          </div>
          <el-button type="primary" round @click="openCreateForOrder(order.id)">
            去评价
          </el-button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <div>
          <h3>我的评价</h3>
          <p>真实评价记录来自服务端</p>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="!list.length" class="eval-empty">
        <el-empty :image-size="80">
          <template #description>
            <p class="empty-title">暂无评价记录</p>
            <p class="empty-desc">
              服务完成后可在「我的订单」或上方待评价列表中提交评价。
            </p>
          </template>
          <el-button type="primary" @click="goEvaluateFromEmpty">去评价 / 查看订单</el-button>
        </el-empty>
      </div>
      <div v-else class="eval-grid">
        <EvaluationCard
          v-for="item in list"
          :key="item.id"
          :evaluation="item"
          @view="openView"
        />
      </div>
    </section>

    <p class="eval-hint">
      说明：仅「已完成」订单可评价；每个订单只能评价一次；评分 1–5 分，内容可选（最多 500 字）。
    </p>

    <EvaluationFormDialog
      v-model:visible="formVisible"
      :mode="formMode"
      :order="formOrder"
      :evaluation="formEvaluation"
      @success="onFormSuccess"
    />
  </div>
</template>

<style scoped>
.eval-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.eval-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.eval-page__head h1 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.eval-page__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.eval-page__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.panel {
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.panel__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.panel__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.stat-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card span {
  font-size: 12px;
  color: #8aa0b5;
}

.stat-card strong {
  font-size: 22px;
  color: #2c4a5e;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pending-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.55);
}

.pending-item strong {
  font-size: 14px;
  color: #2c4a5e;
}

.pending-item p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #718096;
}

.eval-empty {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.eval-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.eval-hint {
  margin: 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .eval-grid,
  .stat-panel {
    grid-template-columns: 1fr;
  }

  .pending-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

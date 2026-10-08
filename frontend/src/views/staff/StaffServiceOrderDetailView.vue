<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  completeMyServiceOrder,
  getMyServiceOrder,
  startMyServiceOrder,
} from '@/api/staff'
import { orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const order = ref(null)
const actionLoading = ref(false)

const stepActive = computed(() => {
  const s = order.value?.status
  if (s === 'PENDING') return 0
  if (s === 'CONFIRMED') return 1
  if (s === 'IN_SERVICE') return 2
  if (s === 'COMPLETED') return 3
  return -1
})

const isCancelled = computed(() => order.value?.status === 'CANCELLED')
const canStart = computed(() => order.value?.status === 'CONFIRMED')
const canComplete = computed(() => order.value?.status === 'IN_SERVICE')

function durationText(m) {
  return m == null ? '-' : `${m} 分钟`
}

async function loadDetail() {
  if (!orderId.value) {
    loadError.value = '无效的订单 ID'
    order.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const res = await getMyServiceOrder(orderId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    order.value = res.data
  } catch (e) {
    order.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在') || msg.includes('无权')) {
      notFound.value = true
      loadError.value = '订单不存在或无权查看'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/staff/services')
}

async function onStart() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确认开始服务订单「${order.value.orderNo || order.value.id}」？`,
      '开始服务',
      { type: 'warning', confirmButtonText: '开始', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    const res = await startMyServiceOrder(order.value.id)
    if (res?.code !== 200) throw new Error(res?.message || '开始失败')
    ElMessage.success('服务已开始')
    await loadDetail()
  } catch {
    // request.js 已提示
  } finally {
    actionLoading.value = false
  }
}

async function onComplete() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确认完成服务订单「${order.value.orderNo || order.value.id}」？完成后不可回退。`,
      '完成服务',
      { type: 'warning', confirmButtonText: '完成', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    const res = await completeMyServiceOrder(order.value.id)
    if (res?.code !== 200) throw new Error(res?.message || '完成失败')
    ElMessage.success('服务已完成')
    await loadDetail()
  } catch {
    // request.js 已提示
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => loadDetail(),
)

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div class="title-row">
          <div class="avatar"><el-icon :size="28"><Document /></el-icon></div>
          <div>
            <h2>{{ order?.orderNo || '服务订单详情' }}</h2>
            <p v-if="order">
              {{ order.elderName || '-' }} · {{ order.serviceName || '-' }}
            </p>
          </div>
        </div>
      </div>
      <div class="page-head__right">
        <el-tag
          v-if="order"
          :type="orderStatusMeta(order.status).type"
          size="large"
        >
          {{ orderStatusMeta(order.status).label }}
        </el-tag>
        <el-button
          v-if="canStart"
          type="success"
          :loading="actionLoading"
          @click="onStart"
        >
          开始服务
        </el-button>
        <el-button
          v-if="canComplete"
          type="warning"
          :loading="actionLoading"
          @click="onComplete"
        >
          完成服务
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :type="notFound ? 'warning' : 'error'"
      :closable="false"
      show-icon
      :title="loadError"
      class="err"
    >
      <template #default>
        <el-button v-if="notFound" type="primary" link @click="goBack">返回列表</el-button>
        <el-button v-else type="primary" link @click="loadDetail">重新加载</el-button>
      </template>
    </el-alert>

    <template v-if="order">
      <div class="panel">
        <h3 class="panel__title">状态进度</h3>
        <el-alert
          v-if="isCancelled"
          type="info"
          :closable="false"
          title="该订单已取消"
          :description="order.cancelReason ? `原因：${order.cancelReason}` : ''"
        />
        <el-steps
          v-else
          :active="stepActive"
          finish-status="success"
          align-center
        >
          <el-step title="待确认" />
          <el-step title="已确认" />
          <el-step title="服务中" />
          <el-step title="已完成" />
        </el-steps>
        <p class="note">
          状态流转：CONFIRMED → 开始服务 → IN_SERVICE → 完成服务 → COMPLETED（后端校验，不可越级）。
        </p>
      </div>

      <div class="panel">
        <h3 class="panel__title">订单信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>订单编号</span><code>{{ order.orderNo || '-' }}</code></div>
          <div class="info-item"><span>订单 ID</span>{{ order.id }}</div>
          <div class="info-item"><span>创建时间</span>{{ order.createdAt || '-' }}</div>
          <div class="info-item"><span>完成时间</span>{{ order.completedAt || '-' }}</div>
          <div class="info-item wide">
            <span>备注</span>{{ order.remark?.trim() || '（无）' }}
          </div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">老人信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>老人姓名</span><strong>{{ order.elderName || '-' }}</strong></div>
          <div class="info-item"><span>老人 ID</span>{{ order.elderId || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">服务信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>服务项目</span><strong>{{ order.serviceName || '-' }}</strong></div>
          <div class="info-item"><span>服务类型</span>{{ serviceTypeLabel(order.serviceType) }}</div>
          <div class="info-item"><span>服务时长</span>{{ durationText(order.durationMinutes) }}</div>
          <div class="info-item"><span>预约开始</span>{{ order.scheduledStartTime || '-' }}</div>
          <div class="info-item"><span>预约结束</span>{{ order.scheduledEndTime || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">执行护理员</h3>
        <div class="info-grid">
          <div class="info-item"><span>护理员</span>{{ order.careStaffName || '-' }}</div>
          <div class="info-item"><span>护理员 ID</span>{{ order.careStaffId || '-' }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 960px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-head__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-head__right {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(91, 184, 176, 0.18);
  color: #357a68;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
}

.err {
  border-radius: 12px;
}

.panel {
  padding: 16px 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.panel__title {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--ec-text);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: var(--ec-text);
}

.info-item span {
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.info-item.wide {
  grid-column: 1 / -1;
  white-space: pre-wrap;
}

.note {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>

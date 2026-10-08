<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelServiceOrder,
  getServiceOrder,
} from '@/api/serviceOrder'
import { getCareServiceItem } from '@/api/careServiceItem'
import { getCareStaff } from '@/api/careStaff'
import { formatPrice, orderStatusMeta, serviceTypeLabel } from '@/utils/familyHome'
import OrderConfirmAssignDrawer from '@/components/admin/OrderConfirmAssignDrawer.vue'

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
const serviceExtra = ref(null)
const staffExtra = ref(null)
const cancelLoading = ref(false)
const confirmVisible = ref(false)

const canConfirm = computed(() => order.value?.status === 'PENDING')
const canCancel = computed(
  () => order.value?.status === 'PENDING' || order.value?.status === 'CONFIRMED',
)

/** Steps：取消态单独展示；正常流 PENDING→COMPLETED */
const stepActive = computed(() => {
  const s = order.value?.status
  if (s === 'PENDING') return 0
  if (s === 'CONFIRMED') return 1
  if (s === 'IN_SERVICE') return 2
  if (s === 'COMPLETED') return 3
  return -1
})

const isCancelled = computed(() => order.value?.status === 'CANCELLED')

function durationText(m) {
  return m == null ? '-' : `${m} 分钟`
}

async function loadOrder() {
  if (!orderId.value) {
    loadError.value = '无效的订单 ID'
    order.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  serviceExtra.value = null
  staffExtra.value = null
  try {
    const res = await getServiceOrder(orderId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    order.value = res.data
    if (order.value?.serviceItemId) {
      loadServiceExtra(order.value.serviceItemId)
    }
    if (order.value?.careStaffId) {
      loadStaffExtra(order.value.careStaffId)
    }
  } catch (e) {
    order.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '订单不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

async function loadServiceExtra(id) {
  try {
    const res = await getCareServiceItem(id)
    if (res?.code === 200) serviceExtra.value = res.data
  } catch {
    serviceExtra.value = null
  }
}

async function loadStaffExtra(id) {
  try {
    const res = await getCareStaff(id)
    if (res?.code === 200) staffExtra.value = res.data
  } catch {
    staffExtra.value = null
  }
}

function goBack() {
  router.push('/admin/orders')
}

function openConfirm() {
  confirmVisible.value = true
}

async function onCancel() {
  if (!order.value) return
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
    cancelLoading.value = true
    const body = {}
    if (value?.trim()) body.cancelReason = value.trim()
    const res = await cancelServiceOrder(order.value.id, body)
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('订单已取消')
    await loadOrder()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    if (!e?.toastShown) ElMessage.error(e.message || '取消失败')
  } finally {
    cancelLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => loadOrder(),
)

onMounted(loadOrder)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div class="title-row">
          <div class="avatar"><el-icon :size="28"><Document /></el-icon></div>
          <div>
            <h2>{{ order?.orderNo || '订单详情' }}</h2>
            <p v-if="order">
              <el-tag :type="orderStatusMeta(order.status).type" size="small">
                {{ orderStatusMeta(order.status).label }}
              </el-tag>
              <span>{{ order.elderName || '-' }} · {{ order.serviceName || '-' }}</span>
            </p>
          </div>
        </div>
      </div>
      <div v-if="order" class="page-head__actions">
        <el-button
          v-if="canConfirm"
          type="primary"
          @click="openConfirm"
        >
          确认并分配护理员
        </el-button>
        <el-button
          v-if="canCancel"
          type="warning"
          :loading="cancelLoading"
          @click="onCancel"
        >
          取消订单
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
        <el-button v-else type="primary" link @click="loadOrder">重新加载</el-button>
      </template>
    </el-alert>

    <template v-if="order">
      <div class="panel">
        <h3 class="panel__title">状态流程</h3>
        <el-alert
          v-if="isCancelled"
          type="info"
          :closable="false"
          show-icon
          :title="`订单已取消${order.cancelReason ? '：' + order.cancelReason : ''}`"
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
        <p class="step-note">管理端不提供开始/完成操作（由护理员端执行）。</p>
      </div>

      <div class="panel">
        <h3 class="panel__title">订单基本信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>订单编号</span><code>{{ order.orderNo }}</code></div>
          <div class="info-item">
            <span>状态</span>
            <el-tag :type="orderStatusMeta(order.status).type" size="small">
              {{ orderStatusMeta(order.status).label }}
            </el-tag>
          </div>
          <div class="info-item">
            <span>订单金额</span>{{ order.amount != null ? `¥${Number(order.amount).toFixed(2)}` : '-' }}
          </div>
          <div class="info-item">
            <span>支付状态</span>
            <el-tag size="small" :type="order.paymentStatus === 'PAID' ? 'success' : 'warning'">
              {{ order.paymentStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </div>
          <div class="info-item"><span>支付时间</span>{{ order.paidAt || '-' }}</div>
          <div class="info-item"><span>创建时间</span>{{ order.createdAt || '-' }}</div>
          <div class="info-item"><span>完成时间</span>{{ order.completedAt || '-' }}</div>
          <div class="info-item"><span>取消时间</span>{{ order.cancelledAt || '-' }}</div>
          <div class="info-item wide"><span>备注</span>{{ order.remark || '-' }}</div>
          <div v-if="order.cancelReason" class="info-item wide">
            <span>取消原因</span>{{ order.cancelReason }}
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
          <div class="info-item">
            <span>参考价格</span>{{ formatPrice(serviceExtra?.price) }}
          </div>
          <div class="info-item wide">
            <span>服务说明</span>{{ serviceExtra?.description || '-' }}
          </div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">预约信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>预约开始</span>{{ order.scheduledStartTime || '-' }}</div>
          <div class="info-item"><span>预约结束</span>{{ order.scheduledEndTime || '-' }}</div>
          <div class="info-item"><span>预计时长</span>{{ durationText(order.durationMinutes) }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">护理员信息</h3>
        <div v-if="order.careStaffId" class="info-grid">
          <div class="info-item">
            <span>护理员</span><strong>{{ order.careStaffName || staffExtra?.name || '-' }}</strong>
          </div>
          <div class="info-item"><span>工号</span>{{ staffExtra?.employeeNo || '-' }}</div>
          <div class="info-item"><span>护理员 ID</span>{{ order.careStaffId }}</div>
          <div class="info-item"><span>岗位</span>{{ staffExtra?.position || '-' }}</div>
        </div>
        <p v-else class="muted">
          未分配护理员。
          <template v-if="canConfirm">请点击「确认并分配护理员」。</template>
        </p>
      </div>
    </template>

    <OrderConfirmAssignDrawer
      v-if="order"
      v-model="confirmVisible"
      :order="order"
      @success="loadOrder"
    />
  </div>
</template>

<style scoped>
.detail-page {
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

.page-head__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-head__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
  color: #3a9a92;
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
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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

.step-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
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
}

.muted {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 14px;
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>

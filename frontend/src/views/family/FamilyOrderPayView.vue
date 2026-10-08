<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getServiceOrder, payServiceOrder } from '@/api/serviceOrder'
import { toastIfNeeded } from '@/api/request'
import {
  unwrap,
  formatPrice,
  formatDateTime,
  orderStatusMeta,
  paymentStatusMeta,
} from '@/utils/familyHome'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const paying = ref(false)
const order = ref(null)
const loadError = ref('')
const paidSuccess = ref(false)

const canPay = computed(
  () =>
    order.value?.status === 'PENDING' &&
    (order.value?.paymentStatus === 'UNPAID' || !order.value?.paymentStatus),
)
const isPaid = computed(() => order.value?.paymentStatus === 'PAID' || paidSuccess.value)

async function load() {
  if (!orderId.value) {
    loadError.value = '订单不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    order.value = unwrap(await getServiceOrder(orderId.value))
    if (order.value?.paymentStatus === 'PAID') {
      paidSuccess.value = true
    }
  } catch (e) {
    order.value = null
    loadError.value = e.message || '加载订单失败'
    toastIfNeeded(e, '加载订单失败')
  } finally {
    loading.value = false
  }
}

async function onPay() {
  if (!order.value || paying.value || !canPay.value) return
  try {
    await ElMessageBox.confirm(
      `确认支付 ${formatPrice(order.value.amount)}？\n当前为系统模拟支付，不产生真实扣款。`,
      '确认支付',
      {
        type: 'warning',
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  paying.value = true
  try {
    await payServiceOrder(order.value.id)
    ElMessage.success('支付成功')
    paidSuccess.value = true
    await load()
  } catch (e) {
    toastIfNeeded(e, '支付失败')
  } finally {
    paying.value = false
  }
}

function goOrders() {
  router.push('/family/orders')
}

function goBack() {
  router.back()
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="pay-page">
    <button type="button" class="back" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </button>

    <header class="pay-page__head">
      <h1>支付订单</h1>
      <p>系统内模拟支付，确认后订单标记为已支付。</p>
    </header>

    <el-skeleton v-if="loading" class="panel" :rows="8" animated />

    <div v-else-if="loadError" class="panel">
      <el-result icon="warning" title="无法支付" :sub-title="loadError">
        <template #extra>
          <el-button type="primary" @click="goOrders">我的订单</el-button>
        </template>
      </el-result>
    </div>

    <section v-else-if="order" class="panel">
      <div v-if="isPaid && !canPay" class="paid-banner">
        <strong>已支付</strong>
        <span>该订单已完成模拟支付</span>
      </div>

      <dl class="pay-meta">
        <div>
          <dt>订单号</dt>
          <dd>{{ order.orderNo || '-' }}</dd>
        </div>
        <div>
          <dt>服务</dt>
          <dd>{{ order.serviceName || '-' }}</dd>
        </div>
        <div>
          <dt>老人</dt>
          <dd>{{ order.elderName || '-' }}</dd>
        </div>
        <div>
          <dt>预约时间</dt>
          <dd>{{ formatDateTime(order.scheduledStartTime) }}</dd>
        </div>
        <div>
          <dt>订单状态</dt>
          <dd>
            <el-tag size="small" :type="orderStatusMeta(order.status).type" effect="light">
              {{ orderStatusMeta(order.status).label }}
            </el-tag>
          </dd>
        </div>
        <div>
          <dt>支付状态</dt>
          <dd>
            <el-tag
              size="small"
              :type="paymentStatusMeta(order.paymentStatus || 'UNPAID').type"
              effect="light"
            >
              {{ paymentStatusMeta(order.paymentStatus || 'UNPAID').label }}
            </el-tag>
          </dd>
        </div>
        <div v-if="order.paidAt">
          <dt>支付时间</dt>
          <dd>{{ formatDateTime(order.paidAt) }}</dd>
        </div>
      </dl>

      <div class="amount-box">
        <span>应付金额</span>
        <strong>{{ formatPrice(order.amount) }}</strong>
      </div>

      <p class="hint">当前为系统模拟支付，取消订单暂不涉及真实退款。</p>

      <div class="actions">
        <el-button
          v-if="canPay"
          type="primary"
          size="large"
          class="pay-btn"
          :loading="paying"
          @click="onPay"
        >
          立即支付
        </el-button>
        <el-button size="large" @click="goOrders">查看订单</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.pay-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}

.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
  margin-bottom: 8px;
}

.pay-page__head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #2c4a5e;
}

.pay-page__head p {
  margin: 0;
  font-size: 13px;
  color: #8aa0b5;
}

.panel {
  margin-top: 16px;
  padding: 20px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.paid-banner {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: rgba(105, 185, 140, 0.12);
  color: #2c4a5e;
}

.paid-banner strong {
  color: #3d9a68;
  font-size: 15px;
}

.pay-meta {
  margin: 0;
  display: grid;
  gap: 10px;
}

.pay-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.55);
}

.pay-meta dt {
  color: #8aa0b5;
  font-size: 13px;
  flex-shrink: 0;
}

.pay-meta dd {
  margin: 0;
  color: #2c4a5e;
  font-size: 14px;
  text-align: right;
  word-break: break-word;
}

.amount-box {
  margin-top: 18px;
  padding: 18px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 244, 232, 0.95), rgba(255, 255, 255, 0.9));
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.amount-box span {
  color: #8aa0b5;
  font-size: 14px;
}

.amount-box strong {
  font-size: 28px;
  color: #c8782a;
}

.hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

.actions {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pay-btn {
  width: 100%;
}
</style>

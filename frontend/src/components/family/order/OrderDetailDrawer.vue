<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  formatDateTime,
  formatPrice,
  orderStatusMeta,
  paymentStatusMeta,
  serviceTypeLabel,
} from '@/utils/familyHome'

const props = defineProps({
  visible: Boolean,
  loading: Boolean,
  order: { type: Object, default: null },
  cancelling: Boolean,
  evaluated: Boolean,
})

const emit = defineEmits(['update:visible', 'cancel', 'evaluate', 'view-evaluation', 'pay'])

const router = useRouter()

const open = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
})

const canCancel = computed(
  () => props.order?.status === 'PENDING' || props.order?.status === 'CONFIRMED',
)
const canPay = computed(
  () =>
    props.order?.status === 'PENDING' &&
    (props.order?.paymentStatus === 'UNPAID' || !props.order?.paymentStatus),
)
const canEvaluate = computed(
  () => props.order?.status === 'COMPLETED' && !props.evaluated,
)
const canViewEvaluation = computed(
  () => props.order?.status === 'COMPLETED' && props.evaluated,
)

function goPay() {
  if (!props.order?.id) return
  emit('pay', props.order)
  open.value = false
  router.push(`/family/orders/${props.order.id}/pay`)
}
</script>

<template>
  <el-drawer v-model="open" title="订单详情" size="440px" destroy-on-close>
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else-if="order">
      <div class="detail-head">
        <h3>{{ order.serviceName || '照护服务' }}</h3>
        <div class="detail-tags">
          <el-tag size="small" :type="orderStatusMeta(order.status).type" effect="light">
            {{ orderStatusMeta(order.status).label }}
          </el-tag>
          <el-tag
            size="small"
            :type="paymentStatusMeta(order.paymentStatus || 'UNPAID').type"
            effect="light"
          >
            {{ paymentStatusMeta(order.paymentStatus || 'UNPAID').label }}
          </el-tag>
        </div>
      </div>

      <el-descriptions :column="1" border class="detail-desc">
        <el-descriptions-item label="订单号">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务对象">{{ order.elderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">
          {{ serviceTypeLabel(order.serviceType) }}
        </el-descriptions-item>
        <el-descriptions-item label="预约开始">
          {{ formatDateTime(order.scheduledStartTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="预约结束">
          {{ formatDateTime(order.scheduledEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="服务时长">
          {{ order.durationMinutes ?? '-' }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span class="price">{{ formatPrice(order.amount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          {{ paymentStatusMeta(order.paymentStatus || 'UNPAID').label }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.paidAt" label="支付时间">
          {{ formatDateTime(order.paidAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="护理员">
          {{ order.careStaffName || '待管理员分配' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(order.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.status === 'CANCELLED'" label="取消时间">
          {{ formatDateTime(order.cancelledAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.status === 'CANCELLED'" label="取消原因">
          {{ order.cancelReason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.status === 'COMPLETED'" label="完成时间">
          {{ formatDateTime(order.completedAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.status === 'COMPLETED'" label="评价状态">
          {{ evaluated ? '已评价' : '待评价' }}
        </el-descriptions-item>
      </el-descriptions>

      <p class="detail-note">
        订单确认、开始与完成由中心工作人员处理。当前为系统模拟支付，取消订单暂不涉及真实退款。
      </p>

      <div
        v-if="canPay || canCancel || canEvaluate || canViewEvaluation"
        class="detail-actions"
      >
        <el-button v-if="canPay" type="warning" @click="goPay">立即支付</el-button>
        <el-button
          v-if="canEvaluate"
          type="warning"
          plain
          @click="emit('evaluate', order)"
        >
          去评价
        </el-button>
        <el-button
          v-if="canViewEvaluation"
          type="primary"
          plain
          @click="emit('view-evaluation', order)"
        >
          查看评价
        </el-button>
        <el-button
          v-if="canCancel"
          type="danger"
          plain
          :loading="cancelling"
          @click="emit('cancel', order)"
        >
          取消预约
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-head h3 {
  margin: 0;
  font-size: 18px;
  color: #2c4a5e;
}

.detail-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.detail-desc {
  --el-descriptions-table-border: rgba(74, 144, 194, 0.12);
}

.price {
  color: #c8782a;
  font-weight: 700;
}

.detail-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: #8aa0b5;
}

.detail-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}
</style>

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
  order: { type: Object, required: true },
  cancelling: Boolean,
  evaluated: Boolean,
})

const emit = defineEmits(['detail', 'cancel', 'evaluate', 'view-evaluation', 'pay'])

const router = useRouter()

const statusMeta = computed(() => orderStatusMeta(props.order.status))
const payMeta = computed(() =>
  paymentStatusMeta(props.order.paymentStatus || 'UNPAID'),
)
const canCancel = computed(
  () => props.order.status === 'PENDING' || props.order.status === 'CONFIRMED',
)
const canPay = computed(
  () =>
    props.order.status === 'PENDING' &&
    (props.order.paymentStatus === 'UNPAID' || !props.order.paymentStatus),
)
const canEvaluate = computed(
  () => props.order.status === 'COMPLETED' && !props.evaluated,
)
const canViewEvaluation = computed(
  () => props.order.status === 'COMPLETED' && props.evaluated,
)

const tone = computed(() => {
  const map = {
    PENDING: 'orange',
    CONFIRMED: 'green',
    IN_SERVICE: 'blue',
    COMPLETED: 'teal',
    CANCELLED: 'gray',
  }
  return map[props.order.status] || 'blue'
})

function goPay() {
  emit('pay', props.order)
  router.push(`/family/orders/${props.order.id}/pay`)
}
</script>

<template>
  <article class="order-card" :class="`order-card--${tone}`">
    <div class="order-card__top">
      <div>
        <h3>{{ order.serviceName || '照护服务' }}</h3>
        <p class="order-no">订单号 {{ order.orderNo || '-' }}</p>
      </div>
      <div class="order-card__tags">
        <el-tag size="small" :type="statusMeta.type" effect="light">
          {{ statusMeta.label }}
        </el-tag>
        <el-tag size="small" :type="payMeta.type" effect="light">
          {{ payMeta.label }}
        </el-tag>
      </div>
    </div>

    <dl class="order-card__meta">
      <div>
        <dt>服务对象</dt>
        <dd>{{ order.elderName || '-' }}</dd>
      </div>
      <div>
        <dt>服务类型</dt>
        <dd>{{ serviceTypeLabel(order.serviceType) }}</dd>
      </div>
      <div>
        <dt>预约开始</dt>
        <dd>{{ formatDateTime(order.scheduledStartTime) }}</dd>
      </div>
      <div>
        <dt>预约结束</dt>
        <dd>{{ formatDateTime(order.scheduledEndTime) }}</dd>
      </div>
      <div>
        <dt>护理员</dt>
        <dd>{{ order.careStaffName || '待管理员分配' }}</dd>
      </div>
      <div>
        <dt>订单金额</dt>
        <dd class="price">{{ formatPrice(order.amount) }}</dd>
      </div>
    </dl>

    <div class="order-card__foot">
      <el-button link type="primary" @click="emit('detail', order)">查看详情</el-button>
      <el-button v-if="canPay" link type="warning" @click="goPay">去支付</el-button>
      <el-button
        v-if="canEvaluate"
        link
        type="warning"
        @click="emit('evaluate', order)"
      >
        去评价
      </el-button>
      <el-button
        v-if="canViewEvaluation"
        link
        type="success"
        @click="emit('view-evaluation', order)"
      >
        查看评价
      </el-button>
      <el-button
        v-if="canCancel"
        link
        type="danger"
        :loading="cancelling"
        @click="emit('cancel', order)"
      >
        取消预约
      </el-button>
    </div>
  </article>
</template>

<style scoped>
.order-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 16px 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.order-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(74, 144, 194, 0.12);
}

.order-card--orange {
  background: linear-gradient(165deg, rgba(255, 244, 232, 0.95), rgba(255, 255, 255, 0.85));
}
.order-card--green {
  background: linear-gradient(165deg, rgba(232, 246, 239, 0.95), rgba(255, 255, 255, 0.85));
}
.order-card--blue {
  background: linear-gradient(165deg, rgba(234, 244, 251, 0.95), rgba(255, 255, 255, 0.85));
}
.order-card--teal {
  background: linear-gradient(165deg, rgba(232, 247, 245, 0.95), rgba(255, 255, 255, 0.85));
}
.order-card--gray {
  background: linear-gradient(165deg, rgba(240, 243, 246, 0.95), rgba(255, 255, 255, 0.85));
}

.order-card__top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.order-card__top h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.order-card__tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.order-no {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8aa0b5;
}

.order-card__meta {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.order-card__meta div {
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.55);
}

.order-card__meta dt {
  font-size: 11px;
  color: #8aa0b5;
  margin-bottom: 2px;
}

.order-card__meta dd {
  margin: 0;
  font-size: 13px;
  color: #2c4a5e;
}

.order-card__meta .price {
  color: #c8782a;
  font-weight: 700;
}

.order-card__foot {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
